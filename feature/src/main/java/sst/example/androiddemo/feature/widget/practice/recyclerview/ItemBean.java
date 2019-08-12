package sst.example.androiddemo.feature.widget.practice.recyclerview;

public class ItemBean {
    public ItemBean(int group, String text) {
        this.group = group;
        this.text = text;
    }

    int group;
    String text;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ItemBean{");
        sb.append("group=").append(group);
        sb.append(", text='").append(text).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
