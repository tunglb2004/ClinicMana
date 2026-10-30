package g2.be.util;

public final class QueuePriorityRules {

    public static final int NORMAL = 0;
    public static final int FOLLOW_UP = 100;
    public static final int RETURNING_FROM_PARACLINICAL = 500;

    private QueuePriorityRules() {
    }

    public static int basePriorityForVisitType(String visitType) {
        return "FOLLOW_UP".equalsIgnoreCase(visitType) ? FOLLOW_UP : NORMAL;
    }
}
