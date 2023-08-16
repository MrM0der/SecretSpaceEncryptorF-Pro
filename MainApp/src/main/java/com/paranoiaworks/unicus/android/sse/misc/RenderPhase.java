package com.paranoiaworks.unicus.android.sse.misc;

public class RenderPhase {

    private int renderPhase = 0;

    public int getRenderPhase(){
        return this.renderPhase;
    }

    public synchronized void setRenderPhase(int renderPhase)
    {
        this.renderPhase = renderPhase;
    }
}
