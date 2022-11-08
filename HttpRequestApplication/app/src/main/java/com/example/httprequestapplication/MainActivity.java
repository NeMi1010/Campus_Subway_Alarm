package com.example.httprequestapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    TextView mainTextView;
    AutoCompleteTextView editUrl;
    Handler handler = new Handler();
    private ArrayList<String> stationlist = new ArrayList<String>();

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

        editUrl = (AutoCompleteTextView) findViewById(R.id.main_editurl);
        editUrl.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, stationlist));
        mainTextView = findViewById(R.id.main_textview);
        mainTextView.setMovementMethod(new ScrollingMovementMethod());

        Button button = findViewById(R.id.main_btn1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                mainTextView.setText("");

                final String urlStr =
                        "http://swopenapi.seoul.go.kr/api/subway/584f69544f746d6439316d63424956/xml/realtimeStationArrival/1/25/"
                                + editUrl.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        request(urlStr);
                    }
                }).start();

                mainTextView.setScrollY(0);
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
                    println("현재 요청 상태 : " + resCode);
                }
            }
        }
        catch (IOException e) {
            println("예외 발생함 : "+e.toString());
        }

        println("N호선 / 열차 방향 / 도착까지 남은 시간 / 안내메세지\n");
        ArrSubway(output.toString());
    }

    public class SubwayData {

        private int SubwayId;           //subwayId, 호선
        private String trainLineId;     //trainLineNm, 열차 방향
        private int arrivalDueTime;     //barvlDt, 도착 남은 시간
        private String arrivalMsg;      //arvlMsg2, 안내메세지

        public void setSubwayId(String arr) {
            this.SubwayId = Integer.parseInt(arr) - 1000;           // 호선 처리 개선 필요함
        }

        public void setTrainLineId(String arr) {
            this.trainLineId = arr;
        }

        public void setArrivalDueTime(String arr) {
            this.arrivalDueTime = Integer.parseInt(arr);
        }

        public void setArrivalMsg(String arr) {
            this.arrivalMsg = arr;
        }

        public StringBuffer getSubwayData() {
            StringBuffer str = new StringBuffer();

            str.append(Integer.toString(SubwayId) + "호선 - ");
            str.append(trainLineId + " - ");
            str.append(Integer.toString(arrivalDueTime / 60) + "분 후 / ");
            str.append(arrivalMsg + "\n");

            return str;
        }
    }

    public void ArrSubway(String data) {
        int rowCount = Integer.parseInt( data.substring(data.indexOf("<total>")+7, data.indexOf("</total>")) );
        int startpt, endpt = 0;
        String rawData;
        SubwayData SWData;

        for(int i = 0; i < rowCount; i++) {
            startpt = data.indexOf("<row>", endpt);
            endpt = data.indexOf("</row>", startpt);
            rawData = data.substring(startpt+5, endpt);

            SWData = new SubwayData();
            SWData.setSubwayId( rawData.substring(rawData.indexOf("<subwayId>")+10, rawData.indexOf("</subwayId")) );
            SWData.setTrainLineId( rawData.substring(rawData.indexOf("<trainLineNm>")+13, rawData.indexOf("</trainLineNm")) );
            SWData.setArrivalDueTime( rawData.substring(rawData.indexOf("<barvlDt>")+9, rawData.indexOf("</barvlDt")) );
            SWData.setArrivalMsg( rawData.substring(rawData.indexOf("<arvlMsg2")+10, rawData.indexOf("</arvlMsg2")) );
            println(SWData.getSubwayData().toString());
        }
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

    public void println(final String data) {
        handler.post(new Runnable() {
            public void run() {
                mainTextView.append(data+"\n");
            }
        });
    }
}