package ru.java.practicum.filmorate.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.java.practicum.filmorate.model.User;
import ru.java.practicum.filmorate.service.UserService;
import ru.java.practicum.filmorate.storage.FriendsStorage;
import ru.java.practicum.filmorate.storage.UserStorage;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDate;


class UserControllerTest {


    protected UserService userService;
    protected UserStorage userStorage;
    protected FriendsStorage friendsStorage;
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();


    @BeforeEach
             void setUp() {
        userService = new UserService(userStorage, friendsStorage);
    }

    @Test
    void validateUserNameNull() {
        User user = User.builder()
                .email("www@mail.ru")
                .name(null)
                .login("Boroda")
                .birthday(LocalDate.of(1999,1,22))
                .build();


        userService.validate(user);
        Assertions.assertEquals(user.getName(), user.getLogin());
    }

    @Test
    void validateUserNameBlank() {
        User user = User.builder()
                .email("www@mail.ru")
                .name("")
                .login("oda")
                .birthday(LocalDate.of(1999,1,22))
                .build();

        validator.validate(user);

        Assertions.assertTrue(user.getName().isBlank());
    }

    @Test
    public void testEmailValidation() {
        User user = User.builder()
                .email("invalidEmail")
                .login("Login")
                .name("totParen")
                .birthday(LocalDate.now())
                .build();


        Assertions.assertFalse(validator.validate(user).isEmpty());
    }

    @Test
    public void testLoginNotBlank() {
        User user = User.builder()
                .email("vlad@mamail.ru")
                .login("")
                .name("name")
                .birthday(LocalDate.now())
                .build();

        Assertions.assertFalse(validator.validate(user).isEmpty());
    }

    @Test
    public void testBirthdayInFuture() {
        User user = User.builder()
                .email("valera@boss.com")
                .login("login")
                .name("nameless")
                .birthday(LocalDate.now().plusDays(1))
                .build();

        Assertions.assertFalse(validator.validate(user).isEmpty());
    }

    @Test
    public void testValidUserOk() {
        User user = User.builder()
                .email("vitalya@smeliy.com")
                .login("login")
                .name("stepasha")
                .birthday(LocalDate.now())
                .build();

        Assertions.assertTrue(validator.validate(user).isEmpty());
    }

}