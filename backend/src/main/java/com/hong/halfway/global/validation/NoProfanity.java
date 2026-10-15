package com.hong.halfway.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 닉네임·방 이름·한줄평처럼 다른 참여자에게 그대로 노출되는 텍스트에 붙여 욕설·비속어를 막음
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoProfanityValidator.class)
public @interface NoProfanity {

    String message() default "사용할 수 없는 단어가 포함되어 있어요.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
