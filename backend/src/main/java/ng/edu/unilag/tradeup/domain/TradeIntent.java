package ng.edu.unilag.tradeup.domain;

/** Whether the lister wants cash, a trade, or will consider either. */
public enum TradeIntent {

    SELL("For sale"),
    SWAP("For swap"),
    BOTH("Sale or swap");

    private final String label;

    TradeIntent(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean acceptsCash() {
        return this == SELL || this == BOTH;
    }

    public boolean acceptsSwap() {
        return this == SWAP || this == BOTH;
    }
}
