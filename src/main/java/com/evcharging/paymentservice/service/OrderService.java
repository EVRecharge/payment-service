package com.evcharging.paymentservice.service;

import com.evcharging.paymentservice.client.BackendClient;
import com.evcharging.paymentservice.client.BookingInfo;
import com.evcharging.paymentservice.dto.OrderResponse;
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
 * Creates the Razorpay Order a booking is paid through. The amount always comes from the
 * booking's own {@code totalCost} as fetched from booking-service -- never from the client -- so a
 * tampered request can't pay less than what's actually owed. When the provider has an activated
 * Route linked account, the order is created with a {@code transfers} instruction so Razorpay
 * splits and settles the provider's share automatically at capture time; otherwise the full amount
 * is held by the platform until the provider completes payout onboarding.
 */
@Service
public class OrderService {

    @Autowired
    private BackendClient backendClient;

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransferCalculator transferCalculator;

    @Autowired
    private PaymentMethodService paymentMethodService;

    @Transactional
    public OrderResponse createOrder(Long bookingId, Long callerUserId, String bearerToken) {
        String customerId = paymentMethodService.resolveOrCreateCustomer(callerUserId, bearerToken);

        Optional<Transaction> existing = transactionRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            Transaction tx = existing.get();
            if (!tx.getUserId().equals(callerUserId)) {
                throw new SecurityException("You can only pay for your own booking");
            }
            if ("CAPTURED".equals(tx.getStatus())) {
                throw new IllegalStateException("This booking has already been paid");
            }
            // Re-open orders (e.g. the driver abandoned checkout) reuse the same Razorpay order.
            return new OrderResponse(bookingId, tx.getRazorpayOrderId(), tx.getAmountPaise(), tx.getCurrency(),
                    razorpayClient.getKeyId(), customerId);
        }

        BookingInfo booking = backendClient.findBooking(bookingId, bearerToken);
        if (booking == null || booking.getBookingId() == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!booking.getUserId().equals(callerUserId)) {
            throw new SecurityException("You can only pay for your own booking");
        }
        if (booking.getTotalCost() == null || booking.getTotalCost() <= 0) {
            throw new IllegalStateException("Booking has no payable amount");
        }

        long amountPaise = Math.round(booking.getTotalCost() * 100);
        Long providerAmountPaise = null;
        Long platformFeePaise = null;
        String providerAccountId = null;

        Optional<TransferCalculator.Transfer> transfer = transferCalculator.computeTransfer(booking.getProviderId(), amountPaise);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amountPaise);
        body.put("currency", "INR");
        body.put("receipt", "booking_" + bookingId);
        body.put("notes", Map.of(
                "bookingId", String.valueOf(bookingId),
                "userId", String.valueOf(booking.getUserId()),
                "providerId", String.valueOf(booking.getProviderId())));

        if (transfer.isPresent()) {
            TransferCalculator.Transfer t = transfer.get();
            providerAccountId = t.getRazorpayAccountId();
            providerAmountPaise = t.getProviderAmountPaise();
            platformFeePaise = t.getPlatformFeePaise();
            body.put("transfers", List.of(Map.of(
                    "account", providerAccountId,
                    "amount", providerAmountPaise,
                    "currency", "INR",
                    "on_hold", 0)));
        }

        Map<String, Object> order = razorpayClient.createOrder(body);
        String razorpayOrderId = (String) order.get("id");

        Transaction tx = new Transaction();
        tx.setBookingId(bookingId);
        tx.setUserId(booking.getUserId());
        tx.setProviderId(booking.getProviderId());
        tx.setRazorpayOrderId(razorpayOrderId);
        tx.setAmountPaise(amountPaise);
        tx.setCurrency("INR");
        tx.setProviderAmountPaise(providerAmountPaise);
        tx.setPlatformFeePaise(platformFeePaise);
        tx.setProviderAccountId(providerAccountId);
        tx.setStatus("CREATED");
        transactionRepository.save(tx);

        return new OrderResponse(bookingId, razorpayOrderId, amountPaise, "INR", razorpayClient.getKeyId(), customerId);
    }
}
