package sst.example.androiddemo.feature.resources;

import android.content.res.AssetManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.util.MyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class XmlParserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xml_parser);
        XmlParse xmlParse = new XmlParse();
        try {
            long start = System.currentTimeMillis();
            InputStream is  = getApplicationContext().getAssets().open("test.xml");
            List<XmlParse.Student> students = xmlParse.dom2xml(is);
            MyUtils.log("time "+(System.currentTimeMillis()-start)+" students dom2xml "+students.toString());
            is  = getApplicationContext().getAssets().open("test.xml");
            start = System.currentTimeMillis();
            students = xmlParse.sax2xml(is);
            MyUtils.log("time "+(System.currentTimeMillis()-start)+" students sax2xml "+students.toString());
            is  = getApplicationContext().getAssets().open("test.xml");
            start = System.currentTimeMillis();
            students = xmlParse.pullXml(is);
            MyUtils.log("time "+(System.currentTimeMillis()-start)+" students pullXml "+students.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
