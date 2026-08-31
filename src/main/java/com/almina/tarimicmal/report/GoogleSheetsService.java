package com.almina.tarimicmal.report;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "Tarim Rapor Botu";

    private Sheets getSheetsService() throws Exception {
        InputStream in = GoogleSheetsService.class.getResourceAsStream("/google-credentials.json");
        if (in == null) {
            throw new RuntimeException("Google kimlik dosyası (google-credentials.json) resources klasöründe bulunamadı!");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }


    public List<List<Object>> readSheetData(String spreadsheetId, String sheetRange) throws Exception {
        Sheets service = getSheetsService();
        Sheets.Spreadsheets.Values.Get request = service.spreadsheets().values().get(spreadsheetId, sheetRange);
        ValueRange response = request.execute();
        return response.getValues(); 
    }


    public void appendRowToSheet(String spreadsheetId, String sheetName, List<Object> rowData) throws Exception {
        Sheets service = getSheetsService();
        
        String range = sheetName + "!A:D";

        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));

        service.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }


    public static String extractSpreadsheetId(String url) {
        if (url == null || !url.contains("/d/")) {
            return url; // Zaten ID formatında olabilir
        }
        try {
            String[] parts = url.split("/d/");
            if (parts.length > 1) {
                String sub = parts[1];
                return sub.split("/")[0];
            }
        } catch (Exception e) {
            // Hata olursa ham halini döndür
        }
        return url;
    }
}