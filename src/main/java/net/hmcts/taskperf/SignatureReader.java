package net.hmcts.taskperf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class SignatureReader {
    public static void main(String a[]) {

        try {
            InputStream stream = SignatureReader.class.getClassLoader().getResourceAsStream("skills.txt");

            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader
                                                        (stream, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }

            List<String> allLines = Arrays.asList(textBuilder.toString().split(","));
            List<Signature> signatures = allLines.stream().map(Signature::new).collect(Collectors.toList());
            System.out.println("Total : " + signatures.size());

            Random ran = new Random();

            signatures.forEach(s -> {
                char c = (char)(ran.nextInt(26) + 'a');
                s.setAuthorisations("" + c);
            });
            signatures.forEach(s -> {
                System.out.print(s.toString());
                System.out.print(",");
            });

            Set<Signature> uniqueSignatureWithoutSkills = new HashSet<>(signatures);
            System.out.println("Unique Signatures Without Skills : " + uniqueSignatureWithoutSkills.size());
            uniqueSignatureWithoutSkills.forEach(s -> System.out.println(s.toString()));

            Set<String> caseIds = signatures.stream().map(Signature::getCaseId).collect(Collectors.toSet());
            System.out.println("Case Ids : " + caseIds.size());
            caseIds.forEach(System.out::println);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
