package net.volcano.island.reservation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.volcano.island.reservation.configuration.ReservationProperties;
import net.volcano.island.reservation.rest.ReservationController;
import net.volcano.island.reservation.rest.model.ReservationDTO;
import net.volcano.island.reservation.rest.model.mapper.ReservationMapper;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@Import(ReservationController.class)
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations="classpath:application-TEST.properties")
public abstract class ReservationBaseTest {
    protected static final LocalDate now = LocalDate.now();
    protected static final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

    protected MockMvc mockMvc;
    protected HttpMessageConverter<Object> mappingJackson2HttpMessageConverter;

    @Autowired
    protected WebApplicationContext webApplicationContext;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected ReservationManager reservationManager;
    @Autowired
    protected ReservationMapper reservationMapper;
    @Autowired
    protected ReservationProperties reservationProperties;

    @Autowired
    protected void setConverters(HttpMessageConverter<Object>[] converters) {
        mappingJackson2HttpMessageConverter = Arrays.stream(converters).filter(
                hmc -> hmc instanceof MappingJackson2HttpMessageConverter).findAny().get();
        assertNotNull("the JSON message converter must not be null", mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * Get all available campsite dates
     */
    protected MvcResult reservationsGetRequest(ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(get("/api/campsite/reservation/availableDates"))
                .andDo(print())
                .andExpect(resultMatcher)
                .andReturn();
    }

    /**
     * POST request - add new reservation
     */
    protected MvcResult reservationsPostRequest(ReservationDTO reservationDTO, ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(post("/api/campsite/reservations")
                        .content(json(reservationDTO))
                        .contentType(contentType))
                .andDo(print())
                .andExpect(resultMatcher)
                .andReturn();
    }

    /**
     * PUT request - update existing reservation
     */
    protected MvcResult reservationPutRequest(String reservationId, ReservationDTO reservationDTO, ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(put("/api/campsite/reservation/{reservationId}", reservationId)
                        .content(json(reservationDTO))
                        .contentType(contentType))
                .andDo(print())
                .andExpect(resultMatcher)
                .andReturn();
    }

    /**
     * Perform delete / cancel reservation request
     */
    protected MvcResult reservationDeleteRequest(String reservationId, String email, ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(delete("/api/campsite/reservation/{reservationId}/{email}", reservationId, email))
                .andExpect(resultMatcher)
                .andReturn();
    }

    /**
     * Perform get reservation request by reservationId
     */
    protected MvcResult reservationGetRequest(String reservationId, ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(get("/api/campsite/reservation/{reservationId}/", reservationId))
                .andExpect(resultMatcher)
                .andReturn();
    }

    protected ReservationDTO getReservationDTOFromResponse(MvcResult mvcResult) throws UnsupportedEncodingException, JsonProcessingException {
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ReservationDTO.class);
    }

    protected List<LocalDate> getListOfDatesFromResponse(MvcResult mvcResult) throws UnsupportedEncodingException, JsonProcessingException {
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {
        });
    }

    protected void assertMessageResponse(String expectedMessage, MvcResult mvcResult) throws UnsupportedEncodingException {
        String messageResponse = mvcResult.getResponse().getContentAsString();
        assertEquals(expectedMessage, messageResponse);
    }

    // Helper method to convert dto to json string
    private String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}
