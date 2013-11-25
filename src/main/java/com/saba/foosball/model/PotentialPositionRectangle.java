package com.saba.foosball.model;

public class PotentialPositionRectangle {
    private int xStart;
    private int xEnd;
    private int yStart;
    private int yEnd;

    public PotentialPositionRectangle(int x, int y) {
        xStart = x - 1;
        xEnd = x + 1;
        yStart = y - 1;
        yEnd = y + 1;
    }

    public boolean isPotentialMember(int x, int y) {
        return x >= xStart && y >= yStart && x <= xEnd && y <= yEnd;
    }

    public boolean isNearby(int x, int y, int within) {
        return x >= xStart - within && y >= yStart - within && x <= xEnd + within && y <= yEnd + within;
    }

    public boolean doesCollide(PotentialPositionRectangle rect) {
        return this.isPotentialMember(rect.getxStart(), rect.getyStart())
                || this.isPotentialMember(rect.getxStart(), rect.getyEnd())
                || this.isPotentialMember(rect.getxEnd(), rect.getyStart())
                || this.isPotentialMember(rect.getxEnd(), rect.getyEnd());
    }

    public boolean isWithin(PotentialPositionRectangle rect, int within) {
        return this.isPotentialMember(rect.getxStart() - within, rect.getyStart() - within)
                || this.isPotentialMember(rect.getxStart() - within, rect.getyEnd() + within)
                || this.isPotentialMember(rect.getxEnd() + within, rect.getyStart() - within)
                || this.isPotentialMember(rect.getxEnd() + within, rect.getyEnd() + within);
    }

    public void addRectangle(PotentialPositionRectangle rect) {
        this.addMember(rect.getxStart(), rect.getyStart());
        this.addMember(rect.getxStart(), rect.getyEnd());
        this.addMember(rect.getxEnd(), rect.getyStart());
        this.addMember(rect.getxEnd(), rect.getyEnd());
    }

    /**
     * A point is a member iff it is in the square or touching it
     */
    public void addMember(int x, int y) {
        if (x <= xStart) {
            xStart = x - 1;
        }
        if (y <= yStart) {
            yStart = y - 1;
        }
        if (x >= xEnd) {
            xEnd = x + 1;
        }
        if (y >= yEnd) {
            yEnd = y + 1;
            ;
        }

    }

    public int getxStart() {
        return xStart;
    }

    public int getxEnd() {
        return xEnd;
    }

    public int getyStart() {
        return yStart;
    }

    public int getyEnd() {
        return yEnd;
    }

    public int getXLen() {
        return xEnd - xStart;
    }

    public int getYLen() {
        return yEnd - yStart;
    }
}
