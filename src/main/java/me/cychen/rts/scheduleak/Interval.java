package me.cychen.rts.scheduleak;

/**
 * Created by CY on 7/13/2015.
 */
public class Interval {
    private long begin;
    private long end;

    public Interval(long inBegin, long inEnd) {
        begin = inBegin;
        end = inEnd;
    }

    // Create a new Interval and duplicate the value from another existing Interval object.
    public Interval(Interval inInterval) {
        this(inInterval.getBegin(), inInterval.getEnd());
    }

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getLength() {
        return (end - begin);
    }

    /* Calculate intersection and return a new Interval object. */
    public Interval intersect(Interval inInterval)
    {
        Interval leftInterval, rightInterval;
        long resultBegin=0, resultEnd=0;

        // Check which one is on the left.
        if (begin <= inInterval.getBegin())
        {// Me is on the left
            leftInterval = this;
            rightInterval = inInterval;
        }
        else
        {
            leftInterval = inInterval;
            rightInterval = this;
        }

        /* Determine begin value. */
        if (leftInterval.getEnd() < rightInterval.getBegin())
        {
            // They have no intersection.
            return null;
        }
        else
        {
            resultBegin = rightInterval.getBegin();
        }

        /* Determine end value. */
        if (leftInterval.getEnd() < rightInterval.getEnd())
        {
            resultEnd = leftInterval.getEnd();
        }
        else
        {
            resultEnd = rightInterval.getEnd();
        }

        return new Interval(resultBegin, resultEnd);
    }

    public void shift(long inOffset)
    {
        begin += inOffset;
        end += inOffset;
    }

    public Boolean contains(long inPoint)
    {
        if ((begin <= inPoint)
                && (end >= inPoint))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

}