package dev.sporebound;

/** Native evolution thresholds, with bounded player bonuses even on long-running worlds. */
public final class EvolutionMath {
    public static final int MAX_POINTS=1_000_000;
    private EvolutionMath(){}
    public static int first(int configured){return Math.max(1,Math.min(MAX_POINTS-1,configured));}
    public static int hyper(int first,int configured){return Math.max(first+1,Math.min(MAX_POINTS,configured));}
    public static int add(int current,int amount){return (int)Math.clamp((long)current+Math.max(0,amount),0,MAX_POINTS);}
    public static int stage(int points,int first,int hyper){return points>=hyper?2:points>=first?1:0;}
    public static String name(int stage){return stage>=2?"Hyper":stage==1?"Evolved":"Symbiont";}
    public static double progress(int points,int first,int hyper){
        return points>=hyper?1:points>=first?(points-first)/(double)(hyper-first):points/(double)first;
    }
    public static double bonus(int points){return Math.clamp(points,0,20);}
    public static float exhaustion(int pieces,int stage){return Math.clamp(pieces,0,4)*0.025f*(1+Math.clamp(stage,0,2)*0.25f);}
}
