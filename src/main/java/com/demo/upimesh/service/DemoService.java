package com.demo.upimesh.service;

import com.demo.upimesh.crypto.HybridCryptoService;
import com.demo.upimesh.crypto.ServerKeyHolder;
import com.demo.upimesh.model.Account;
import com.demo.upimesh.model.AccountRepository;
import com.demo.upimesh.model.MeshPacket;
import com.demo.upimesh.model.PaymentInstruction;
import com.demo.upimesh.model.TransactionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Helper service that:
 *   - seeds demo accounts on startup
 *   - simulates "sender phone creates an encrypted packet" flow
 */
@Service
public class DemoService {

    private static final Logger log = LoggerFactory.getLogger(DemoService.class);

    @Autowired private AccountRepository accounts;
    @Autowired private HybridCryptoService crypto;
    @Autowired private ServerKeyHolder serverKey;
    @Autowired private TransactionRepository transactions;
    @Autowired private MeshSimulatorService mesh;
    @Autowired private IdempotencyService idempotency;

    @PostConstruct
    public void seedAccounts() throws Exception {
        if (accounts.count() == 0) {
            accounts.save(new Account("alice@demo", "Alice",   new BigDecimal("5000.00"), sha256Hex("1234")));
            accounts.save(new Account("bob@demo",   "Bob",     new BigDecimal("1000.00"), sha256Hex("1234")));
            accounts.save(new Account("carol@demo", "Carol",   new BigDecimal("2500.00"), sha256Hex("1234")));
            accounts.save(new Account("dave@demo",  "Dave",    new BigDecimal("500.00"), sha256Hex("1234")));
            log.info("Seeded 4 demo accounts");
        }
    }

    /**
     * Resets the complete demo state so a fresh test can start from the
     * original seeded balances and an empty ledger/mesh.
     */
    public synchronized void resetDemo() {
        transactions.deleteAll();
        mesh.resetMesh();
        idempotency.clear();

        resetAccount("alice@demo", "Alice", new BigDecimal("5000.00"));
        resetAccount("bob@demo", "Bob", new BigDecimal("1000.00"));
        resetAccount("carol@demo", "Carol", new BigDecimal("2500.00"));
        resetAccount("dave@demo", "Dave", new BigDecimal("500.00"));

        log.info("Demo state reset to original seeded balances");
    }

    private void resetAccount(String vpa, String name, BigDecimal balance) {
        Account account = accounts.findById(vpa).orElseGet(() -> new Account(vpa, name, balance));
        account.setHolderName(name);
        account.setBalance(balance);
        if (account.getPinHash() == null) {
            try {
                account.setPinHash(sha256Hex("1234"));
            } catch (Exception e) {
                throw new IllegalStateException("Could not restore demo PIN", e);
            }
        }
        accounts.save(account);
    }

    /**
     * Simulates the sender's phone:
     *   1. Build a PaymentInstruction with a fresh nonce + signedAt timestamp.
     *   2. Encrypt with the server's public key (hybrid RSA+AES).
     *   3. Wrap in a MeshPacket with TTL.
     *
     * In a real Android app, this exact code (minus the server-side reference)
     * would run on the phone. The phone would have already cached the server's
     * public key during a previous online session.
     */
    public MeshPacket createPacket(String senderVpa, String receiverVpa,
                                   BigDecimal amount, String pin, int ttl) throws Exception {
        PaymentInstruction instruction = new PaymentInstruction(
                senderVpa,
                receiverVpa,
                amount,
                sha256Hex(pin),
                UUID.randomUUID().toString(),       // nonce — guarantees uniqueness
                Instant.now().toEpochMilli()        // signedAt — for freshness check
        );

        String ciphertext = crypto.encrypt(instruction, serverKey.getPublicKey());

        MeshPacket packet = new MeshPacket();
        packet.setPacketId(UUID.randomUUID().toString());
        packet.setTtl(ttl);
        packet.setCreatedAt(Instant.now().toEpochMilli());
        packet.setCiphertext(ciphertext);
        return packet;
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
