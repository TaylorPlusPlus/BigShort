package taylor.programming.bootmoneymanager2.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.StringTokenizer;

@Getter
@Setter
@Slf4j
public class PurchasedStock {

    // == properties ==
    private int investmentID;
    private String symbol;
    private double purchase_price;
    private String purchaseDate;
    private double numberOfShares;
    private double currentPrice;
    private double totalGainsAndLoses;
    private double percentChange;
    private String date;
    private String dailyDate;

    // == constructors ==
    public PurchasedStock(){

    }

    public PurchasedStock(int investmentID, String symbol, double purchase_price, String purchaseDate, double numberOfShares) {
        this.investmentID = investmentID;
        this.symbol = symbol;
        this.purchase_price = purchase_price;
        this.purchaseDate = purchaseDate;
        this.numberOfShares = numberOfShares;
    }

    // == methods ==
    public void setTotalGainsAndLoses(){
        totalGainsAndLoses = (currentPrice - purchase_price)  * numberOfShares;
    }

    /* =======================setCurrentPriceData method ===========================================================
        This method makes an API call to retrieve current stock information
        input: API information
        output: stock data
     */
    public void setCurrentPriceData(String APIprefix, String APIkey){

        String querySymbol = getSymbol().trim();
        String query = APIprefix + querySymbol + "&" + APIkey;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(query))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseDaily)  // TODO MAYBE PROBLEM WITH THIS!

                .join();

    }
    // ======================== END setCurrentPriceData ==============================================================


    /* =======================setCurrentPriceData method ===========================================================
        This is used to parson the JSON text provided by the API
        input: JSON response text
        output: sets current price of a stock
     */
    public String parseDaily(String responseBody){

        String parsedDate = "";
        date = timeConverter();

        JSONObject allData = new JSONObject(responseBody);
        JSONObject metaData = allData.getJSONObject("Meta Data");
        dailyDate = metaData.getString("3. Last Refreshed");
        JSONObject timeSeries = allData.getJSONObject("Time Series (Daily)");

        // Some stocks display as "Date Time" and others display "Date", the date is tokenized to
        // always only receive date.

        StringTokenizer tokenizer = new StringTokenizer(dailyDate, " ", false);
        parsedDate = tokenizer.nextToken();

        JSONObject todaysStockData = timeSeries.getJSONObject(parsedDate);

        currentPrice = (todaysStockData.getDouble("4. close"));


        return null;
    }

    // ============ the following 2 methods are no longer in use ====================================================
    public String timeConverter() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        String date = "";
        String time = "";
        String hour = "";
        String minute = "";
        String second = "";

        StringTokenizer tok = new StringTokenizer(now.toString(), "T");
        date = tok.nextToken();
        time = tok.nextToken();


        StringTokenizer timeTok = new StringTokenizer(time, ":");
        hour = timeTok.nextToken();
        minute = timeTok.nextToken();
        second = timeTok.nextToken();

        if (Integer.parseInt(hour) > 18) {
            return date;
        }else{
            return dateConverter(date);
        }
    }

    public String dateConverter(String date){

        String returnDate ="";
        StringTokenizer dateTok = new StringTokenizer(date, "-");
        String year = dateTok.nextToken();
        String month = dateTok.nextToken();
        String day = dateTok.nextToken();
        int yearInt;
        int monthInt;
        int dayInt;

        if(day.equals("01")){
            //code to deal with month conversion
            if(month.equals("01")){
                month = "12";
                day = "31";
                yearInt = Integer.parseInt(year);
                yearInt--;
                year = String.valueOf(yearInt);
            }else{
                monthInt = Integer.parseInt(month);
                monthInt--;
                switch(monthInt){
                    case 1:
                    case 3:
                    case 12:
                    case 10:
                    case 8:
                    case 7:
                    case 5:
                        day ="31";
                        break;
                    case 2:
                        day = "28";
                        break;
                    case 4:
                    case 11:
                    case 9:
                    case 6:
                        day = "30";
                        break;

                }
                month = String.valueOf(monthInt);
            }
        }else{

            dayInt = Integer.parseInt(day);
            dayInt--;
            if(dayInt < 10){
                day = "0" + String.valueOf(dayInt);
            }else {
                day = String.valueOf(dayInt);
            }
        }
        return year + "-" + month + "-" + day;
    }

}

