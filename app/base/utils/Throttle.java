package base.utils;

import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.atomic.AtomicLong;


public class Throttle {
    public enum Result{
        Proceed,Stop,OverLimit
    }

    private final AtomicLong counter;
    private final long limit;
    private final long timeSpanMilliseconds;
    private final AtomicLong resetTime;

    public Throttle(final long limit, final long timeSpanMilliseconds){
        this.limit = limit;
        this.timeSpanMilliseconds = timeSpanMilliseconds;
        this.resetTime = new AtomicLong(currentTimestamp()+timeSpanMilliseconds);
        this.counter = new AtomicLong();
        reset();
    }

    public Throttle(final long limit, final FiniteDuration duration){
        this(limit,duration.toMillis());
    }

    private long currentTimestamp(){return System.currentTimeMillis();}
    private void reset(){
        counter.set(0);
        resetTime.set(currentTimestamp()+timeSpanMilliseconds);
    }

    public Result get(){
        if(resetTime.get()<currentTimestamp()){
            reset();
        }
        final long currentCounter = counter.incrementAndGet();
        if(currentCounter==limit){
            return Result.Stop;
        } else if(currentCounter>limit){
            return Result.OverLimit;
        } else {
            return Result.Proceed;
        }
    }

}
