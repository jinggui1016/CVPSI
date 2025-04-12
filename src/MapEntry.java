import org.bouncycastle.math.ec.ECPoint;

class MapEntry {
    private String x;

    private ECPoint hx;

    public MapEntry(String x, ECPoint hx) {
        this.x = x;
        this.hx = hx;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public ECPoint getHx() {
        return hx;
    }

    public void setHx(ECPoint hx) {
        this.hx = hx;
    }
}
