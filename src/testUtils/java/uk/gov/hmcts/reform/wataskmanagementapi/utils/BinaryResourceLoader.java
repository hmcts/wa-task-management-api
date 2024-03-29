package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class BinaryResourceLoader {

    private BinaryResourceLoader() {
        // noop
    }

    public static Map<String, Resource> load(String locationPattern) throws IOException {

        Resource[] resources =
            new PathMatchingResourcePatternResolver()
                .getResources(locationPattern);

        return
            Stream
                .of(resources)
                .collect(Collectors.toMap(
                    Resource::getFilename,
                    Function.identity(),
                    (u, v) -> {
                        throw new IllegalStateException(String.format("Duplicate key %s", u));
                    },
                    TreeMap::new
                ));
    }
}
