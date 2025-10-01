/*
 * Copyright (c) 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.package.common.mapper;

import java.nio.file.Path;
import java.util.Currency;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.Duration;
import java.time.Period;
import java.net.URI;
import java.net.URL;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import org.mapstruct.Named;

public class CommonConverters {

    @Named("uuidToString")
    public String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    @Named("stringToUUID")
    public UUID stringToUUID(String string) {
        return string != null ? UUID.fromString(string) : null;
    }

    @Named("bigDecimalToString")
    public String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toString() : null;
    }

    @Named("stringToBigDecimal")
    public BigDecimal stringToBigDecimal(String value) {
        return value != null ? new BigDecimal(value) : null;
    }

    @Named("bigIntegerToString")
    public String bigIntegerToString(BigInteger value) {
        return value != null ? value.toString() : null;
    }

    @Named("stringToBigInteger")
    public BigInteger stringToBigInteger(String value) {
        return value != null ? new BigInteger(value) : null;
    }

    @Named("currencyToString")
    public String currencyToString(Currency currency) {
        return currency != null ? currency.getCurrencyCode() : null;
    }

    @Named("stringToCurrency")
    public Currency stringToCurrency(String code) {
        return code != null ? Currency.getInstance(code) : null;
    }

    @Named("pathToString")
    public String pathToString(Path path) {
        return path != null ? path.toString() : null;
    }

    @Named("stringToPath")
    public Path stringToPath(String string) {
        return string != null ? Path.of(string) : null;
    }

    @Named("localDateTimeToString")
    public String localDateTimeToString(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toString() : null;
    }

    @Named("stringToLocalDateTime")
    public LocalDateTime stringToLocalDateTime(String string) {
        return string != null ? LocalDateTime.parse(string) : null;
    }

    @Named("localDateToString")
    public String localDateToString(LocalDate localDate) {
        return localDate != null ? localDate.toString() : null;
    }

    @Named("stringToLocalDate")
    public LocalDate stringToLocalDate(String string) {
        return string != null ? LocalDate.parse(string) : null;
    }

    @Named("offsetDateTimeToString")
    public String offsetDateTimeToString(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toString() : null;
    }

    @Named("stringToOffsetDateTime")
    public OffsetDateTime stringToOffsetDateTime(String string) {
        return string != null ? OffsetDateTime.parse(string) : null;
    }

    @Named("zonedDateTimeToString")
    public String zonedDateTimeToString(ZonedDateTime zonedDateTime) {
        return zonedDateTime != null ? zonedDateTime.toString() : null;
    }

    @Named("stringToZonedDateTime")
    public ZonedDateTime stringToZonedDateTime(String string) {
        return string != null ? ZonedDateTime.parse(string) : null;
    }

    @Named("instantToString")
    public String instantToString(Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    @Named("stringToInstant")
    public Instant stringToInstant(String string) {
        return string != null ? Instant.parse(string) : null;
    }

    @Named("durationToString")
    public String durationToString(Duration duration) {
        return duration != null ? duration.toString() : null;
    }

    @Named("stringToDuration")
    public Duration stringToDuration(String string) {
        return string != null ? Duration.parse(string) : null;
    }

    @Named("periodToString")
    public String periodToString(Period period) {
        return period != null ? period.toString() : null;
    }

    @Named("stringToPeriod")
    public Period stringToPeriod(String string) {
        return string != null ? Period.parse(string) : null;
    }

    @Named("uriToString")
    public String uriToString(URI uri) {
        return uri != null ? uri.toString() : null;
    }

    @Named("stringToUri")
    public URI stringToUri(String string) {
        return string != null ? URI.create(string) : null;
    }

    @Named("urlToString")
    public String urlToString(URL url) {
        return url != null ? url.toString() : null;
    }

    @Named("stringToUrl")
    public URL stringToUrl(String string) {
        try {
            return string != null ? new URL(string) : null;
        } catch (Exception e) {
            throw new RuntimeException("Invalid URL: " + string, e);
        }
    }

    @Named("fileToString")
    public String fileToString(File file) {
        return file != null ? file.getPath() : null;
    }

    @Named("stringToFile")
    public File stringToFile(String string) {
        return string != null ? new File(string) : null;
    }

    @Named("atomicIntegerToString")
    public String atomicIntegerToString(AtomicInteger atomicInteger) {
        return atomicInteger != null ? String.valueOf(atomicInteger.get()) : null;
    }

    @Named("stringToAtomicInteger")
    public AtomicInteger stringToAtomicInteger(String string) {
        return string != null ? new AtomicInteger(Integer.parseInt(string)) : null;
    }

    @Named("atomicLongToString")
    public String atomicLongToString(AtomicLong atomicLong) {
        return atomicLong != null ? String.valueOf(atomicLong.get()) : null;
    }

    @Named("stringToAtomicLong")
    public AtomicLong stringToAtomicLong(String string) {
        return string != null ? new AtomicLong(Long.parseLong(string)) : null;
    }
    
    @Named("listToString")
    public String listToString(List<String> list) {
        return list != null ? String.join(",", list) : null;
    }
    
    @Named("stringToList")
    public List<String> stringToList(String string) {
        return string != null ? java.util.Arrays.asList(string.split(",")) : null;
    }
}