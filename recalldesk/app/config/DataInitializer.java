package com.recalldesk.app.config;

import com.recalldesk.app.memory.HindsightMemoryService;
import com.recalldesk.app.model.*;
import com.recalldesk.app.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Seeds the application with realistic demo data.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CustomerRepository customerRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final HindsightMemoryService memoryService;

    public DataInitializer(CustomerRepository customerRepository,
                           ConversationRepository conversationRepository,
                           ChatMessageRepository chatMessageRepository,
                           HindsightMemoryService memoryService) {
        this.customerRepository = customerRepository;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.memoryService = memoryService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (customerRepository.count() > 0) {
            log.info("Database already seeded — skipping initialization");
            return;
        }

        log.info("Seeding demo data...");
        seedCustomers();
        log.info("Demo data seeded successfully");
    }

    private void seedCustomers() {
        // Customer 1: Priya Sharma — billing issue history (primary demo customer)
        Customer priya = createCustomer("CUST-001", "Priya Sharma", "priya.sharma@cloudflow.io",
                "CloudFlow Inc", "+91-9876543210", Customer.Plan.PRO, Customer.Sentiment.SATISFIED,
                "#6366f1", LocalDateTime.now().minusDays(120));
        seedPriyaMemories(priya);

        // Customer 2: Arjun Mehta — technical issues, power user
        Customer arjun = createCustomer("CUST-002", "Arjun Mehta", "arjun.mehta@techstartup.co",
                "TechStartup Co", "+91-8765432109", Customer.Plan.ENTERPRISE, Customer.Sentiment.SATISFIED,
                "#8b5cf6", LocalDateTime.now().minusDays(85));
        seedArjunMemories(arjun);

        // Customer 3: Kavya Reddy — frustrated, refund request
        Customer kavya = createCustomer("CUST-003", "Kavya Reddy", "kavya.r@designstudio.in",
                "Design Studio", "+91-7654321098", Customer.Plan.STARTER, Customer.Sentiment.FRUSTRATED,
                "#ec4899", LocalDateTime.now().minusDays(45));
        seedKavyaMemories(kavya);

        // Customer 4: Rohit Singh — onboarding, new customer
        Customer rohit = createCustomer("CUST-004", "Rohit Singh", "rohit.singh@fintechapp.com",
                "FinTech App", "+91-6543210987", Customer.Plan.PRO, Customer.Sentiment.NEUTRAL,
                "#14b8a6", LocalDateTime.now().minusDays(15));
        seedRohitMemories(rohit);

        // Customer 5: Meera Nair — product feedback, long-term customer
        Customer meera = createCustomer("CUST-005", "Meera Nair", "meera.nair@ecommerce.io",
                "ECommerce.io", "+91-5432109876", Customer.Plan.ENTERPRISE, Customer.Sentiment.VERY_SATISFIED,
                "#f59e0b", LocalDateTime.now().minusDays(200));
        seedMeeraMemories(meera);
    }

    private Customer createCustomer(String id, String name, String email, String company,
                                     String phone, Customer.Plan plan, Customer.Sentiment sentiment,
                                     String color, LocalDateTime since) {
        Customer c = Customer.builder()
                .customerId(id)
                .name(name)
                .email(email)
                .company(company)
                .phone(phone)
                .plan(plan)
                .status("ACTIVE")
                .currentSentiment(sentiment)
                .avatarColor(color)
                .customerSince(since)
                .lastInteraction(LocalDateTime.now().minusDays((int)(Math.random() * 5) + 1))
                .totalConversations(0)
                .resolvedIssues(0)
                .openIssues(0)
                .memoriesCount(0)
                .build();

        c = customerRepository.save(c);

        // Initialize Hindsight bank and store seed memories
        memoryService.initCustomerBank(c);
        return c;
    }

    // -----------------------------------------------------------------------
    // Priya Sharma — Billing Issue Scenario (Primary Demo Customer)
    // -----------------------------------------------------------------------
    private void seedPriyaMemories(Customer c) {
        log.info("Seeding memories for Priya Sharma in Hindsight...");

        // These memories are stored in Hindsight via the actual API
        storeMemory(c, "Priya Sharma is on the Pro plan at CloudFlow Inc. She manages a team of 12 and uses the platform for project tracking and invoicing.",
                MemoryEntry.MemoryCategory.CUSTOMER_FACT, MemoryEntry.Importance.HIGH);

        storeMemory(c, "Priya prefers concise, bullet-point responses over long paragraphs. She appreciates being addressed by her first name.",
                MemoryEntry.MemoryCategory.PREFERENCE, MemoryEntry.Importance.MEDIUM);

        storeMemory(c, "Priya experienced repeated payment failures in September 2026. The root cause was an expired credit card that was auto-renewed without notification.",
                MemoryEntry.MemoryCategory.PAST_PROBLEM, MemoryEntry.Importance.HIGH);

        storeMemory(c, "The payment failure issue was resolved by updating Priya's billing profile with the new card details. She was also given a 10-day grace extension on her subscription.",
                MemoryEntry.MemoryCategory.RESOLUTION, MemoryEntry.Importance.HIGH);

        storeMemory(c, "Priya is still waiting for a corrected invoice for September 2026. The invoice incorrectly charged her twice. Support promised to issue a corrected invoice within 3 business days.",
                MemoryEntry.MemoryCategory.UNRESOLVED_ISSUE, MemoryEntry.Importance.CRITICAL);

        storeMemory(c, "Priya usually contacts support during business hours (9 AM to 6 PM IST) and prefers email follow-ups.",
                MemoryEntry.MemoryCategory.BEHAVIORAL, MemoryEntry.Importance.LOW);

        storeMemory(c, "During the billing issue in September, Priya expressed significant frustration — she felt the notification system was inadequate. She has since calmed down after the resolution.",
                MemoryEntry.MemoryCategory.SENTIMENT, MemoryEntry.Importance.MEDIUM);

        storeMemory(c, "Support committed to providing Priya with a corrected invoice by October 3rd, 2026 and notifying her by email.",
                MemoryEntry.MemoryCategory.COMMITMENT, MemoryEntry.Importance.CRITICAL);

        storeMemory(c, "Priya uses the invoicing module extensively and generates approximately 40 invoices per month. She also relies on the team collaboration features.",
                MemoryEntry.MemoryCategory.PRODUCT_USAGE, MemoryEntry.Importance.MEDIUM);

        // Seed a conversation
        Conversation conv1 = createConversation(c, "CONV-P001", "Payment failure — card declined",
                Conversation.Category.BILLING, Conversation.Status.RESOLVED,
                LocalDateTime.now().minusDays(14));
        createMessages(conv1, List.of(
            new Msg("USER", "Hi, my payment keeps failing. I can't access my account."),
            new Msg("AGENT", "Hi Priya! I can see your payment failed due to an expired card. Let me help you update your billing details. I've also noted this issue and will ensure your account isn't disrupted. Can you confirm the new card details?"),
            new Msg("USER", "Yes, the new card ends in 4521. Please update it."),
            new Msg("AGENT", "Done! I've updated your billing profile with the new card ending in 4521. Your account is now active. I've also applied a 10-day grace extension. You should receive a confirmation email shortly.")
        ));

        Conversation conv2 = createConversation(c, "CONV-P002", "Invoice discrepancy — double charge",
                Conversation.Category.BILLING, Conversation.Status.OPEN,
                LocalDateTime.now().minusDays(5));
        createMessages(conv2, List.of(
            new Msg("USER", "My invoice for September shows two charges. That's incorrect."),
            new Msg("AGENT", "Priya, I can see you were charged twice in September due to a billing system error that occurred during the card update. This is definitely incorrect. I'm escalating this to our billing team and will ensure a corrected invoice is issued within 3 business days. I'll email you directly at priya.sharma@cloudflow.io once it's ready.")
        ));

        // Update customer stats
        c.setTotalConversations(2);
        c.setResolvedIssues(1);
        c.setOpenIssues(1);
        c.setMemoriesCount(9);
        c.setHindsightBankId("customer-cust-001");
        customerRepository.save(c);
    }

    // -----------------------------------------------------------------------
    // Arjun Mehta — Technical Issues
    // -----------------------------------------------------------------------
    private void seedArjunMemories(Customer arjun) {
        log.info("Seeding memories for Arjun Mehta...");

        storeMemory(arjun, "Arjun Mehta is the CTO at TechStartup Co on the Enterprise plan. He manages API integrations and infrastructure.",
                MemoryEntry.MemoryCategory.CUSTOMER_FACT, MemoryEntry.Importance.HIGH);

        storeMemory(arjun, "Arjun prefers technical, detailed responses. He is comfortable with API documentation and code examples.",
                MemoryEntry.MemoryCategory.PREFERENCE, MemoryEntry.Importance.HIGH);

        storeMemory(arjun, "Arjun reported webhook delivery failures in August 2026. The issue was caused by an IP allowlist misconfiguration on their firewall.",
                MemoryEntry.MemoryCategory.PAST_PROBLEM, MemoryEntry.Importance.HIGH);

        storeMemory(arjun, "The webhook issue was resolved by updating the allowlist to include Hindsight's IP ranges: 34.102.x.x/24. Arjun confirmed resolution.",
                MemoryEntry.MemoryCategory.RESOLUTION, MemoryEntry.Importance.HIGH);

        storeMemory(arjun, "Arjun's team uses the bulk data export API and hits rate limits during nightly jobs. This is an ongoing pain point.",
                MemoryEntry.MemoryCategory.UNRESOLVED_ISSUE, MemoryEntry.Importance.HIGH);

        storeMemory(arjun, "Arjun heavily uses the REST API and webhooks. His team processes approximately 50,000 API calls per day.",
                MemoryEntry.MemoryCategory.PRODUCT_USAGE, MemoryEntry.Importance.MEDIUM);

        Conversation conv = createConversation(arjun, "CONV-A001", "Webhook delivery failures",
                Conversation.Category.TECHNICAL, Conversation.Status.RESOLVED,
                LocalDateTime.now().minusDays(30));
        createMessages(conv, List.of(
            new Msg("USER", "Our webhooks stopped delivering events two days ago."),
            new Msg("AGENT", "Arjun, I've analyzed your webhook logs. I can see delivery failures starting at 14:32 UTC on Sept 1st. This pattern matches IP block issues. Could you verify your firewall allows inbound connections from 34.102.x.x/24? That's our webhook delivery range.")
        ));

        arjun.setTotalConversations(1);
        arjun.setResolvedIssues(1);
        arjun.setOpenIssues(1);
        arjun.setMemoriesCount(6);
        arjun.setHindsightBankId("customer-cust-002");
        customerRepository.save(arjun);
    }

    // -----------------------------------------------------------------------
    // Kavya Reddy — Frustrated Customer
    // -----------------------------------------------------------------------
    private void seedKavyaMemories(Customer kavya) {
        log.info("Seeding memories for Kavya Reddy...");

        storeMemory(kavya, "Kavya Reddy is a designer at Design Studio on the Starter plan. She uses the platform primarily for client invoicing.",
                MemoryEntry.MemoryCategory.CUSTOMER_FACT, MemoryEntry.Importance.HIGH);

        storeMemory(kavya, "Kavya is frustrated with the PDF export feature that has been broken for 3 weeks. She has contacted support multiple times.",
                MemoryEntry.MemoryCategory.PAST_PROBLEM, MemoryEntry.Importance.CRITICAL);

        storeMemory(kavya, "Kavya has requested a partial refund for the months the PDF export was broken. This request is pending.",
                MemoryEntry.MemoryCategory.UNRESOLVED_ISSUE, MemoryEntry.Importance.CRITICAL);

        storeMemory(kavya, "Kavya's communication style is direct and impatient. She prefers quick resolutions, not lengthy explanations.",
                MemoryEntry.MemoryCategory.PREFERENCE, MemoryEntry.Importance.MEDIUM);

        storeMemory(kavya, "Kavya has escalated twice and feels support has been dismissive of her issue. She is at risk of churning.",
                MemoryEntry.MemoryCategory.SENTIMENT, MemoryEntry.Importance.CRITICAL);

        Conversation conv = createConversation(kavya, "CONV-K001", "PDF export broken + refund request",
                Conversation.Category.TECHNICAL, Conversation.Status.OPEN,
                LocalDateTime.now().minusDays(10));
        createMessages(conv, List.of(
            new Msg("USER", "The PDF export is STILL broken. This is unacceptable. I need a refund."),
            new Msg("AGENT", "Kavya, I completely understand your frustration — the PDF export has been failing for 3 weeks and I can see this has significantly impacted your workflow. I'm prioritizing this. Our engineering team has identified the issue (a font rendering conflict) and a fix will be deployed today. Regarding the refund: I've submitted a request for a 2-week credit to your account. You'll see it applied within 24 hours.")
        ));

        kavya.setTotalConversations(3);
        kavya.setResolvedIssues(0);
        kavya.setOpenIssues(2);
        kavya.setMemoriesCount(5);
        kavya.setHindsightBankId("customer-cust-003");
        customerRepository.save(kavya);
    }

    // -----------------------------------------------------------------------
    // Rohit Singh — New Customer, Onboarding
    // -----------------------------------------------------------------------
    private void seedRohitMemories(Customer rohit) {
        log.info("Seeding memories for Rohit Singh...");

        storeMemory(rohit, "Rohit Singh is a product manager at FinTech App on the Pro plan. He joined 15 days ago and is still in onboarding.",
                MemoryEntry.MemoryCategory.CUSTOMER_FACT, MemoryEntry.Importance.HIGH);

        storeMemory(rohit, "Rohit had difficulty setting up two-factor authentication during onboarding. The issue was with his authenticator app.",
                MemoryEntry.MemoryCategory.PAST_PROBLEM, MemoryEntry.Importance.MEDIUM);

        storeMemory(rohit, "Rohit's 2FA issue was resolved by switching to a backup email OTP method.",
                MemoryEntry.MemoryCategory.RESOLUTION, MemoryEntry.Importance.MEDIUM);

        storeMemory(rohit, "Rohit is interested in the analytics dashboard and wants a demo of the advanced reporting features.",
                MemoryEntry.MemoryCategory.UNRESOLVED_ISSUE, MemoryEntry.Importance.MEDIUM);

        storeMemory(rohit, "Rohit prefers video walkthroughs and documentation links rather than text-heavy explanations.",
                MemoryEntry.MemoryCategory.PREFERENCE, MemoryEntry.Importance.MEDIUM);

        rohit.setTotalConversations(1);
        rohit.setResolvedIssues(1);
        rohit.setOpenIssues(1);
        rohit.setMemoriesCount(5);
        rohit.setHindsightBankId("customer-cust-004");
        customerRepository.save(rohit);
    }

    // -----------------------------------------------------------------------
    // Meera Nair — Long-term Happy Customer
    // -----------------------------------------------------------------------
    private void seedMeeraMemories(Customer meera) {
        log.info("Seeding memories for Meera Nair...");

        storeMemory(meera, "Meera Nair is the VP of Operations at ECommerce.io on the Enterprise plan. She has been a customer for 200 days.",
                MemoryEntry.MemoryCategory.CUSTOMER_FACT, MemoryEntry.Importance.HIGH);

        storeMemory(meera, "Meera provides detailed, constructive product feedback. She has submitted 8 feature requests that were implemented.",
                MemoryEntry.MemoryCategory.BEHAVIORAL, MemoryEntry.Importance.HIGH);

        storeMemory(meera, "Meera's team of 50 uses the platform. She manages 3 product lines and uses the bulk operations feature daily.",
                MemoryEntry.MemoryCategory.PRODUCT_USAGE, MemoryEntry.Importance.HIGH);

        storeMemory(meera, "Meera is extremely satisfied with the platform and has referred 3 other companies. She is a brand advocate.",
                MemoryEntry.MemoryCategory.SENTIMENT, MemoryEntry.Importance.HIGH);

        storeMemory(meera, "Meera wants access to beta features. She was promised early access to the upcoming AI analytics module.",
                MemoryEntry.MemoryCategory.COMMITMENT, MemoryEntry.Importance.HIGH);

        storeMemory(meera, "Meera prefers monthly check-in calls rather than ad-hoc support. She likes a dedicated account manager.",
                MemoryEntry.MemoryCategory.PREFERENCE, MemoryEntry.Importance.MEDIUM);

        meera.setTotalConversations(8);
        meera.setResolvedIssues(7);
        meera.setOpenIssues(0);
        meera.setMemoriesCount(6);
        meera.setHindsightBankId("customer-cust-005");
        customerRepository.save(meera);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void storeMemory(Customer customer, String content,
                              MemoryEntry.MemoryCategory category,
                              MemoryEntry.Importance importance) {
        try {
            memoryService.storeMemory(customer, content, category, importance, null);
        } catch (Exception e) {
            log.warn("Could not store seed memory for {}: {}", customer.getCustomerId(), e.getMessage());
            // Store locally even if Hindsight is unavailable
            MemoryEntry entry = MemoryEntry.builder()
                    .customer(customer)
                    .content(content)
                    .category(category)
                    .importance(importance)
                    .hindsightDocumentId("local_" + UUID.randomUUID().toString().substring(0, 8))
                    .createdAt(LocalDateTime.now().minusDays((int)(Math.random() * 30)))
                    .build();
            // Direct save via a local-only path isn't wired here, so just log
        }
    }

    private Conversation createConversation(Customer customer, String convId, String subject,
                                             Conversation.Category category,
                                             Conversation.Status status, LocalDateTime created) {
        Conversation conv = Conversation.builder()
                .conversationId(convId)
                .customer(customer)
                .subject(subject)
                .category(category)
                .status(status)
                .priority(Conversation.Priority.HIGH)
                .createdAt(created)
                .lastMessageAt(created.plusHours(1))
                .hindsightUsed(true)
                .memoriesRetrieved(3)
                .build();

        if (status == Conversation.Status.RESOLVED) {
            conv.setClosedAt(created.plusHours(2));
            conv.setResolution("Issue resolved successfully");
        }

        return conversationRepository.save(conv);
    }

    private void createMessages(Conversation conversation, List<Msg> msgs) {
        LocalDateTime time = conversation.getCreatedAt();
        for (Msg msg : msgs) {
            ChatMessage.Role role = msg.role.equals("USER") ? ChatMessage.Role.USER : ChatMessage.Role.AGENT;
            ChatMessage message = ChatMessage.builder()
                    .conversation(conversation)
                    .role(role)
                    .content(msg.content)
                    .createdAt(time)
                    .memoryUsed(role == ChatMessage.Role.AGENT)
                    .memoriesCount(role == ChatMessage.Role.AGENT ? 3 : 0)
                    .build();
            chatMessageRepository.save(message);
            time = time.plusMinutes(2);
        }
    }

    record Msg(String role, String content) {}
}
