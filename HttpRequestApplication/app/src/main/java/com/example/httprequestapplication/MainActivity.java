package com.example.httprequestapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
//    TextView mainTextView;
    AutoCompleteTextView editUrl;
    Handler handler = new Handler();
    private ArrayList<String> stationlist = new ArrayList<String>();
    private ListAdapter myAdapter;
    Thread myProcess;

    private void setMyAdapter(ListAdapter adapt) { this.myAdapter = adapt; }

    private void setStationlist(String myinput) {
        this.stationlist.add(myinput);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            readDataFromCsv("SWSTATION_20221028.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }

        editUrl = (AutoCompleteTextView) findViewById(R.id.main_editUrl);
        editUrl.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, stationlist));
//        mainTextView = findViewById(R.id.main_textview);
//        mainTextView.setMovementMethod(new ScrollingMovementMethod());

        Button button = findViewById(R.id.main_btn1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

//                mainTextView.setText("");

                final String urlStr =
                        "http://swopenapi.seoul.go.kr/api/subway/584f69544f746d6439316d63424956/xml/realtimeStationArrival/1/25/"
                                + editUrl.getText().toString();
                myProcess = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        request(urlStr);
                    }
                });

                myProcess.start();
//                mainTextView.setScrollY(0);

                try {
                    myProcess.join();
                } catch (InterruptedException e) {

                }

                ListView listView = (ListView) findViewById(R.id.main_listview);
                listView.setAdapter(myAdapter);
                myAdapter.notifyDataSetChanged();
            }

        });
    }

    public void request(String urlStr) {
        StringBuffer output = new StringBuffer();
        try {
            URL url = new URL(urlStr);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if(connection != null) {
                connection.setConnectTimeout(10000);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);

                String line = null;
                int resCode = connection.getResponseCode();
                if (resCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    line = reader.readLine();

                    output.append(line.replace("><", ">\n<"));
                    reader.close();
                    connection.disconnect();
                }
                else {
                    System.out.println("현재 요청 상태 : " + resCode);
                }
            }
        }
        catch (IOException e) {
            System.out.println("예외 발생함 : "+e.toString());
        }

        ArrSubway(output.toString());
    }

    public class SubwayData {

        private int SubwayId;           //subwayId, 호선
        private String trainLineId;     //trainLineNm, 열차 방향
        private int arrivalDueTime;     //barvlDt, 도착 남은 시간
        private String arrivalMsg;      //arvlMsg2, 안내메세지

        public SubwayData(String rawData) {
            this.SubwayId = Integer.parseInt( rawData.substring(rawData.indexOf("<subwayId>")+10, rawData.indexOf("</subwayId")) ) - 1000;
            this.trainLineId = rawData.substring(rawData.indexOf("<trainLineNm>")+13, rawData.indexOf("</trainLineNm"));
            this.arrivalDueTime = Integer.parseInt( rawData.substring(rawData.indexOf("<barvlDt>")+9, rawData.indexOf("</barvlDt")) );
            this.arrivalMsg = rawData.substring(rawData.indexOf("<arvlMsg2")+10, rawData.indexOf("</arvlMsg2"));
        }

        public int getSubwayId() {
            return this.SubwayId;
        }

        public List4Data getViewData() {
            String resPath = "@drawable/metro" + this.SubwayId;
            String myPackage = MainActivity.this.getPackageName();
            List4Data mylist = new List4Data(
                    getResources().getIdentifier(resPath, "drawable", myPackage),
                    this.trainLineId,
                    this.arrivalDueTime / 60 + "분 후 / " + arrivalMsg
            );

            return mylist;
        }

/*      public StringBuffer getSubwayData() {
            StringBuffer str = new StringBuffer();

            str.append(Integer.toString(SubwayId) + "호선 - ");
            str.append(trainLineId + " - ");
            str.append(Integer.toString(arrivalDueTime / 60) + "분 후 / ");
            str.append(arrivalMsg + "\n");

            return str;
        }*/
    }

    public class myComparator implements Comparator<SubwayData> {
        @Override
        public int compare(SubwayData d1, SubwayData d2) {
            if(d1.getSubwayId() < d2.getSubwayId()) return -1;
            if(d1.getSubwayId() > d2.getSubwayId()) return 1;
            return 0;
        }
    }

    public void ArrSubway(String data) {
        int rowCount = Integer.parseInt( data.substring(data.indexOf("<total>")+7, data.indexOf("</total>")) );
        int startpt, endpt = 0;
        String rawData;
        SubwayData SWData;
        ArrayList<SubwayData> SWData_list = new ArrayList<SubwayData>();
        ArrayList<List4Data> convertlist = new ArrayList<List4Data>();

        for(int i = 0; i < rowCount; i++) {
            startpt = data.indexOf("<row>", endpt);
            endpt = data.indexOf("</row>", startpt);
            rawData = data.substring(startpt+5, endpt);

            SWData = new SubwayData(rawData);
            SWData_list.add(SWData);
        }
        SWData_list.sort(new myComparator());
        for(SubwayData X : SWData_list) {
            convertlist.add(X.getViewData());
        }

        ListAdapter listAdapter = new ListAdapter(this, convertlist);
        setMyAdapter(listAdapter);
    }

    public void readDataFromCsv(String filePath) throws IOException {
        InputStreamReader CsvReader = new InputStreamReader(
                getAssets().open(filePath) );
        BufferedReader reader = new BufferedReader(CsvReader);
        String line;
        String [] data;

        while((line = reader.readLine()) != null) {
            data = line.split(",");
            setStationlist(data[2]);
        }
    }
/*
    public void println(final String data) {
        handler.post(new Runnable() {
            public void run() {
                mainTextView.append(data+"\n");
            }
        });
    }
*/
    public class List4Data {

        private int ImageResource;
        private String StationName;
        private String others;

        public List4Data(int ID, String Name, String Details) {
            this.ImageResource = ID;
            this.StationName = Name;
            this.others = Details;
        }

        public int getImage() {
            return this.ImageResource;
        }

        public String getStationName() {
            return this.StationName;
        }

        public String getOthers() {
            return this.others;
        }
    }

    public class ListAdapter extends BaseAdapter {
        Context myContext = null;
        LayoutInflater myLayoutInflater = null;
        ArrayList<List4Data> myData;

        public ListAdapter(Context context, ArrayList<List4Data> listData) {
            myContext = context;
            myLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            myData = listData;
        }

        @Override
        public View getView(int pos, View view, ViewGroup parent) {
            View currentView = myLayoutInflater.inflate(R.layout.listview_custom, parent, false);

            ImageView imageView = (ImageView)currentView.findViewById(R.id.linear_ImageView);
            TextView stationName = (TextView)currentView.findViewById(R.id.StationName);
            TextView Details = (TextView)currentView.findViewById(R.id.Details);

            imageView.setImageResource(myData.get(pos).getImage());
            stationName.setText(myData.get(pos).getStationName());
            Details.setText(myData.get(pos).getOthers());

            return currentView;
        }

        @Override
        public int getCount() {
            return myData.size();
        }

        @Override
        public Object getItem(int pos) {
            return myData.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        public void addItem(int imagesrc, String Name, String DT) {
            List4Data item;
            item = new List4Data(imagesrc, Name, DT);

            myData.add(item);
        }

    }

}