package com.lumie.common.api;

import com.lumie.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import static org.assertj.core.api.Assertions.*;

class SortFieldAllowlistInterceptorTest {
    static class Endpoint {
        @AllowedSortFields({"rank", "totalScore"}) public void list() {}
    }
    private boolean accepts(String... sorts) throws Exception {
        var request = new MockHttpServletRequest();
        request.addParameter("sort", sorts);
        return new SortFieldAllowlistInterceptor().preHandle(request, new MockHttpServletResponse(),
                new HandlerMethod(new Endpoint(), Endpoint.class.getMethod("list")));
    }
    @Test void rejectsUnallowedSecondPropertyInSameParameter() {
        assertThatThrownBy(() -> accepts("rank,privateField,desc")).isInstanceOf(BusinessException.class);
    }
    @Test void acceptsAllKnownPropertiesAndDirectionModifiers() throws Exception {
        assertThat(accepts("rank,totalScore,desc,ignorecase")).isTrue();
        assertThat(accepts("rank,asc", "totalScore,desc")).isTrue();
    }
}
