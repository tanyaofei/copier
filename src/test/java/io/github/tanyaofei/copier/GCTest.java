package io.github.tanyaofei.copier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author tanyaofei
 * @since 2025/7/2
 **/
public class GCTest extends Assertions {

    @Test
    public void test() throws InterruptedException {
        Copiers.copy(new A("a", "b"), A.class);

        System.gc();
        Thread.sleep(300);

        assertTrue(Copiers.COPIERS.isEmpty());
        assertTrue(Generator.CACHE.isEmpty());
    }


    public record A(
            String a,
            String b
    ) {

    }


}
