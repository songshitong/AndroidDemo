package sst.example.androiddemo.feature.widget.practice.recyclerview.customLayoutManager;

public class ItemViewInfo {

    private int mLeft;
    private float mScale;

    public ItemViewInfo(int left, float scale) {
        this.mLeft = left;
        this.mScale = scale;
    }

    public void setLeft(int left) {
        this.mLeft = left;
    }

    public void setScale(float scale) {
        this.mScale = scale;
    }

    public int getLeft() {
        return mLeft;
    }

    public float getScale() {
        return mScale;
    }
}
