
通用的组件
气泡，提示view
MarkerView
```
MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view); //marker的布局
mv.setChartView(chart);
chart.setMarker(mv);
```

x轴，y轴
```
XAxis xAxis = chart.getXAxis();
YAxis yAxis = chart.getAxisLeft(); //左侧的y轴
yAxis.setAxisMaximum(200f); //数值区间
yAxis.setAxisMinimum(-50f);
enableGridDashedLine() // 垂直于对应轴的虚线----
```

限制线 LimitLine  图表的上限或下限
```
LimitLine ll1 = new LimitLine(150f, "Upper Limit");
ll1.setLineWidth(4f);
ll1.enableDashedLine(10f, 10f, 0f);
ll1.setLabelPosition(LimitLabelPosition.RIGHT_TOP);
ll1.setTextSize(10f);
yAxis.addLimitLine(ll1);
```

描述文字Description   默认为"Description Label"，图表的右下角
```
chart.getDescription().setEnabled(true);
chart.getDescription().setText("this is Description");
```

图例 Legend     例如：黄色·代表男，红色·代表女 默认在左下角
```
Legend l = chart.getLegend();
l.setYEntrySpace(10f);
l.setWordWrapEnabled(true);
LegendEntry l1=new LegendEntry("Male",Legend.LegendForm.CIRCLE,10f,2f,null,Color.YELLOW);
LegendEntry l2=new LegendEntry("Female", Legend.LegendForm.CIRCLE,10f,2f,null,Color.RED);
l.setCustom(new LegendEntry[]{l1,l2});
l.setEnabled(true);

//图例的样式 线形，圆形等
setForm(LegendForm.LINE);
```


数据DataSet 一组或某种类型数据，柱状图是一组一组的展示
不同的DataSet可以设置条目的样式 例如：setHighLightColor  可查看https://github.com/PhilJay/MPAndroidChart/wiki/DataSet-classes-in-detail
图数据ChartData  chart的数据
以线段图为例
```
LineDataSet set1 = new LineDataSet(values, "DataSet 1");
set1.setLineWidth(1f);
set1.setValueTextSize(9f);
set1.setDrawFilled(true);
ArrayList<ILineDataSet> dataSets = new ArrayList<>();
dataSets.add(set1); 
LineData data = new LineData(dataSets);
chart.setData(data);
```

高亮Highlight
监听选中
chart.setOnChartValueSelectedListener(this);
高亮的样式设置
```
dataSet.setHighlightEnabled(true); // allow highlighting for DataSet
dataSet.setDrawHighlightIndicators(true); // set this to false to disable the drawing of highlight indicator (lines)
dataSet.setHighlightColor(Color.BLACK); // color for highlight indicator
```
高亮某个条目
```
Highlight highlight = new Highlight(50f, 0);  // highlight the entry and x-position 50 in the first (0) DataSet
chart.highlightValue(highlight, false);// highlight this value, don't call listener
```

动画Animations
动画时间针对x或y
```
mChart.animateX(3000); // animate horizontal 3000 milliseconds
mChart.animateY(3000); // animate vertical 3000 milliseconds
mChart.animateXY(3000, 3000); 
```
动画的曲线  Easing.EasingOption.Linear等
```
mChart.animateY(3000, Easing.EasingOption.Linear); 
```


图表用法：
折线图
填充内容(线条下面的)渐变
```
Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);//drawable配置渐变色
LineDataSet.setFillDrawable(drawable);
```
线条渐变色
```
 chart.post(new Runnable() {
                @Override public void run() {
                    Paint paint = chart.getRenderer().getPaintRender();
                    int height = chart.getHeight();
                    LinearGradient linGrad = new LinearGradient(0, 0, 0, height,
                        getResources().getColor(android.R.color.holo_orange_dark),
                        getResources().getColor(android.R.color.holo_purple),
                        Shader.TileMode.REPEAT);
                    paint.setShader(linGrad);
                    chart.invalidate();
                }
            });
```


柱状图
渐变色
gradientFills.add(new Fill(startColor1, endColor1));
BarDataSet.setFills(gradientFills);