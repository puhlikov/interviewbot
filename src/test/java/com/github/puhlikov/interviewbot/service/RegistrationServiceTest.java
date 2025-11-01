package com.github.puhlikov.interviewbot.service;

import com.github.puhlikov.interviewbot.enums.RegistrationState;
import com.github.puhlikov.interviewbot.enums.SettingsState;
import com.github.puhlikov.interviewbot.model.BotUser;
import com.github.puhlikov.interviewbot.repo.BotUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private BotUserRepository userRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private BotUser testUser;
    private static final Long TEST_CHAT_ID = 12345L;

    @BeforeEach
    void setUp() {
        testUser = new BotUser();
        testUser.setId(1L);
        testUser.setChatId(TEST_CHAT_ID);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setUsername("johndoe");
        testUser.setRegistrationState(RegistrationState.COMPLETED);
    }

    @Test
    void testStartRegistration_Success() {
        // Arrange
        ArgumentCaptor<BotUser> userCaptor = ArgumentCaptor.forClass(BotUser.class);
        when(userRepository.save(any(BotUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BotUser result = registrationService.startRegistration(TEST_CHAT_ID);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(userCaptor.capture());
        BotUser savedUser = userCaptor.getValue();
        assertEquals(TEST_CHAT_ID, savedUser.getChatId());
        assertEquals(RegistrationState.FIRST_NAME, savedUser.getRegistrationState());
    }

    @Test
    void testGetUserByChatId_Success() {
        // Arrange
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(testUser));

        // Act
        Optional<BotUser> result = registrationService.getUserByChatId(TEST_CHAT_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_CHAT_ID, result.get().getChatId());
        verify(userRepository, times(1)).findByChatId(TEST_CHAT_ID);
    }

    @Test
    void testGetUserByChatId_NotFound() {
        // Arrange
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.empty());

        // Act
        Optional<BotUser> result = registrationService.getUserByChatId(TEST_CHAT_ID);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateFirstName_Success() {
        // Arrange
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(BotUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BotUser result = registrationService.updateFirstName(TEST_CHAT_ID, "Jane");

        // Assert
        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
        assertEquals(RegistrationState.LAST_NAME, result.getRegistrationState());
        verify(userRepository, times(1)).save(any(BotUser.class));
    }

    @Test
    void testUpdateLastName_Success() {
        // Arrange
        testUser.setRegistrationState(RegistrationState.LAST_NAME);
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(BotUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BotUser result = registrationService.updateLastName(TEST_CHAT_ID, "Smith");

        // Assert
        assertNotNull(result);
        assertEquals("Smith", result.getLastName());
        assertEquals(RegistrationState.USERNAME, result.getRegistrationState());
    }

    @Test
    void testUpdateUsername_Success() {
        // Arrange
        testUser.setRegistrationState(RegistrationState.USERNAME);
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(BotUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BotUser result = registrationService.updateUsername(TEST_CHAT_ID, "janesmith");

        // Assert
        assertNotNull(result);
        assertEquals("janesmith", result.getUsername());
        assertEquals(RegistrationState.SCHEDULE_TIME, result.getRegistrationState());
    }

    @Test
    void testUpdateScheduleTime_Success() {
        // Arrange
        testUser.setRegistrationState(RegistrationState.SCHEDULE_TIME);
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(BotUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BotUser result = registrationService.updateScheduleTime(TEST_CHAT_ID, "14:30");

        // Assert
        assertNotNull(result);
        assertEquals(LocalTime.of(14, 30), result.getScheduleTime());
        assertEquals(RegistrationState.TIMEZONE, result.getRegistrationState());
    }

    @Test
    void testUpdateScheduleTime_InvalidFormat() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            registrationService.updateScheduleTime(TEST_CHAT_ID, "invalid");
        });
    }

    @Test
    void testUpdateTimezone_Success() {
        // Arrange
        testUser.setRegistrationState(RegistrationState.TIMEZONE);
        testUser.setScheduleTime(LocalTime.of(14, 0));
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(BotUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BotUser result = registrationService.updateTimezone(TEST_CHAT_ID, "Europe/Moscow");

        // Assert
        assertNotNull(result);
        assertEquals("Europe/Moscow", result.getTimezone());
        assertEquals(RegistrationState.COMPLETED, result.getRegistrationState());
        assertEquals(20, result.getQuestionsPerSession());
    }

    @Test
    void testUpdateQuestionsPerSession_Success() {
        // Arrange
        registrationService.startQuestionsCountSetting(TEST_CHAT_ID);
        assertTrue(registrationService.isInSettingsState(TEST_CHAT_ID, SettingsState.AWAITING_QUESTIONS_COUNT));
        
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(BotUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BotUser result = registrationService.updateQuestionsPerSession(TEST_CHAT_ID, "15");

        // Assert
        assertNotNull(result);
        assertEquals(15, result.getQuestionsPerSession());
        assertFalse(registrationService.isInSettingsState(TEST_CHAT_ID, SettingsState.AWAITING_QUESTIONS_COUNT));
    }

    @Test
    void testUpdateQuestionsPerSession_InvalidNumber() {
        // Act & Assert - exception is thrown before repository call
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registrationService.updateQuestionsPerSession(TEST_CHAT_ID, "51");
        });
        assertTrue(exception.getMessage().contains("1 до 50"));
    }

    @Test
    void testUpdateQuestionsPerSession_InvalidFormat() {
        // Act & Assert - NumberFormatException happens before repository call
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registrationService.updateQuestionsPerSession(TEST_CHAT_ID, "not_a_number");
        });
        assertTrue(exception.getMessage().contains("корректное число"));
    }

    @Test
    void testSettingsStateManagement() {
        // Test startQuestionsCountSetting
        registrationService.startQuestionsCountSetting(TEST_CHAT_ID);
        assertTrue(registrationService.isInSettingsState(TEST_CHAT_ID, SettingsState.AWAITING_QUESTIONS_COUNT));

        // Test clearSettingsState
        registrationService.clearSettingsState(TEST_CHAT_ID);
        assertFalse(registrationService.isInSettingsState(TEST_CHAT_ID, SettingsState.AWAITING_QUESTIONS_COUNT));
    }

    @Test
    void testGetTimezoneKeyboard() {
        // Act
        var keyboard = registrationService.getTimezoneKeyboard();

        // Assert
        assertNotNull(keyboard);
        assertNotNull(keyboard.getKeyboard());
        assertTrue(keyboard.getResizeKeyboard());
        assertTrue(keyboard.getOneTimeKeyboard());
    }
}

