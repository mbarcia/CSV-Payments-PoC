package poc.csvfile;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import poc.Command;
import poc.csvrecord.PaymentRecord;
import poc.csvrecord.PaymentRecordBean;
import poc.csvrecord.SendPaymentCommand;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReadFileCommand implements Command<CSVPaymentsFile> {
    @Override
    public void execute(CSVPaymentsFile csvFile) {
        try {
            Reader reader = new BufferedReader(new FileReader(csvFile.getPath().toString()));

            CsvToBean<PaymentRecordBean> csvReader = new CsvToBeanBuilder<PaymentRecordBean>(reader)
                    .withType(PaymentRecordBean.class)
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            List<PaymentRecordBean> results = csvReader.parse();

            for (PaymentRecordBean paymentRecordBean : results) {
                // Use OpenCSV to parse the line into a bean
                PaymentRecord paymentRecord = (new PaymentRecord(paymentRecordBean));
                // Commands creation
                Command<PaymentRecord> sendPaymentCommand = new SendPaymentCommand();
                // Commands list assignment
                paymentRecord.setCommandList(List.of(sendPaymentCommand));
                // Make it all happen
                paymentRecord.forEach(command -> command.execute(paymentRecord));
            }

        } catch (FileNotFoundException ex) {
            Logger logger = Logger.getLogger("CSVPaymentsFile");
            logger.log(Level.SEVERE, ex.getLocalizedMessage());
        }
    }
}