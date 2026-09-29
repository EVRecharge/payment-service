package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.dto.ChargeBookingRequest;
import com.evcharging.paymentservice.entity.SavedPaymentMethod;
import com.evcharging.paymentservice.entity.Transaction;
import com.evcharging.paymentservice.razorpay.RazorpayClient;
import com.evcharging.paymentservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Charges a driver's saved primary payment method the moment a provider confirms their booking --
 * the actual point of intent in this product ("book now, pay once accepted"), rather than charging
 * at booking time. If the driver has no saved method yet, this simply doesn't charge (the booking
 * still gets confirmed); {@link OrderService}'s interactive checkout remains available from My
 * Bookings as a manual fallback.
 */
@Service
public class BookingChargeService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentMethodService paymentMethodService;

    @Autowired
    private TransferCalculator transferCalculator;

    @Autowired
    private RazorpayClient razorpayClient;

    @Transactional
    public void chargeOnConfirmation(ChargeBookingRequest request) {
        if (transactionRepository.findByBookingId(request.getBookingId())
                .map(tx -> "CAPTURED".equals(tx.getStatus()))
                .orElse(false)) {
            return; // already paid (e.g. a manual "Pay now" beat this to it) -- never double-charge
        }

        Optional<SavedPaymentMethod> primary = paymentMethodService.getPrimaryMethod(request.getUserId());
        Optional<String> customerId = paymentMethodService.getCustomerId(request.getUserId());
        if (primary.isEmpty() || customerId.isEmpty()) {
            throw new IllegalStateException("Driver has no saved payment method");
        }

        long amountPaise = Math.round(request.getAmountRupees() * 100);
        Optional<TransferCalculator.Transfer> transfer =
                transferCalculator.computeTransfer(request.getProviderId(), amountPaise);

        Map<String, Object> orderBody = new HashMap<>();
        orderBody.put("amount", amountPaise);
        orderBody.put("currency", "INR");
        orderBody.put("receipt", "booking_" + request.getBookingId());
        orderBody.put("notes", Map.of(
                "bookingId", String.valueOf(request.getBookingId()),
                "userId", String.valueOf(request.getUserId()),
                "providerId", String.valueOf(request.getProviderId())));

        String providerAccountId = null;
        Long providerAmountPaise = null;
        Long platformFeePaise = null;
        if (transfer.isPresent()) {
            TransferCalculator.Transfer t = transfer.get();
            providerAccountId = t.getRazorpayAccountId();
            providerAmountPaise = t.getProviderAmountPaise();
            platformFeePaise = t.getPlatformFeePaise();
            orderBody.put("transfers", List.of(Map.of(
                    "account", providerAccountId,
                    "amount", providerAmountPaise,
                    "currency", "INR",
                    "on_hold", 0)));
        }

        Map<String, Object> order = razorpayClient.createOrder(orderBody);
        String razorpayOrderId = (String) order.get("id");

        Transaction tx = new Transaction();
        tx.setBookingId(request.getBookingId());
        tx.setUserId(request.getUserId());
        tx.setProviderId(request.getProviderId());
        tx.setRazorpayOrderId(razorpayOrderId);
        tx.setAmountPaise(amountPaise);
        tx.setCurrency("INR");
        tx.setProviderAmountPaise(providerAmountPaise);
        tx.setPlatformFeePaise(platformFeePaise);
        tx.setProviderAccountId(providerAccountId);
        tx.setStatus("CREATED");
        transactionRepository.save(tx);

        Map<String, Object> customer = razorpayClient.fetchCustomer(customerId.get());

        Map<String, Object> chargeBody = new HashMap<>();
        chargeBody.put("amount", amountPaise);
        chargeBody.put("currency", "INR");
        chargeBody.put("order_id", razorpayOrderId);
        chargeBody.put("customer_id", customerId.get());
        chargeBody.put("email", customer.getOrDefault("email", ""));
        chargeBody.put("contact", customer.getOrDefault("contact", ""));
        chargeBody.put("description", "EVRecharge India booking #" + request.getBookingId());

        Map<String, Object> payment;
        try {
            payment = razorpayClient.chargeSavedToken(primary.get().getRazorpayTokenId(), chargeBody);
        } catch (Exception e) {
            tx.setStatus("FAILED");
            tx.setFailureReason("Auto-charge failed: " + e.getMessage());
            transactionRepository.save(tx);
            throw new IllegalStateException("Could not charge the driver's saved payment method", e);
        }

        tx.setRazorpayPaymentId((String) payment.get("id"));
        if ("captured".equals(payment.get("status"))) {
            tx.setStatus("CAPTURED");
        }
        transactionRepository.save(tx);
    }
}
