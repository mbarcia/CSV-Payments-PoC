package poc.csvfile;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import poc.Command;
import poc.csvrecord.PaymentRecord;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ReadFileCommand implements Command<CSVPaymentsFile, Stream<PaymentRecord>> {
    @Override
    public Stream<PaymentRecord> execute(CSVPaymentsFile csvFile) {
        try {
            Reader reader = new BufferedReader(new FileReader(csvFile.getPath()));

            CsvToBean<PaymentRecord> csvReader = new CsvToBeanBuilder<PaymentRecord>(reader)
                    .withType(PaymentRecord.class)
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            return csvReader.parse().stream().map(record -> record.setFilepath(csvFile.getPath()));

        } catch (FileNotFoundException ex) {
            Logger logger = Logger.getLogger(String.valueOf(CSVPaymentsFile.class));
            logger.log(Level.SEVERE, ex.getLocalizedMessage());
        }
        return null;
    }
}