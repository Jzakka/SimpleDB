package com.ll;

import org.junit.jupiter.api.Test;

public class something {

    @Test
    void test() {
        Class<Integer> integerClass = Integer.class;
        System.out.println("integerClass.getSimpleName() = " + integerClass.getSimpleName());

        Class<Integer> intClass = int.class;
        System.out.println("int.class.getSimpleName() = " + intClass.getSimpleName());
    }
}
