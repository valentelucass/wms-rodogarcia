package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginTunnelOriginTest {
    @ParameterizedTest
    @CsvSource(
            value = {
                "https://primeiro.brs.devtunnels.ms,primeiro.brs.devtunnels.ms,127.0.0.1,true",
                "https://segundo.brs.devtunnels.ms,segundo.brs.devtunnels.ms,127.0.0.1,true",
                "https://primeiro.brs.devtunnels.ms,segundo.brs.devtunnels.ms,127.0.0.1,false",
                "https://fora.invalid,fora.invalid,127.0.0.1,false",
                "https://primeiro.brs.devtunnels.ms.evil.invalid,primeiro.brs.devtunnels.ms.evil.invalid,127.0.0.1,false",
                "http://primeiro.brs.devtunnels.ms,primeiro.brs.devtunnels.ms,127.0.0.1,false",
                "https://primeiro.brs.devtunnels.ms,primeiro.brs.devtunnels.ms,192.0.2.1,false",
                "http://localhost:59999,localhost:59999,127.0.0.1,true",
                "http://localhost:59998,localhost:59999,127.0.0.1,false",
                "null,primeiro.brs.devtunnels.ms,127.0.0.1,false",
                "https://primeiro.brs.devtunnels.ms/path,primeiro.brs.devtunnels.ms,127.0.0.1,false",
                "https://user@primeiro.brs.devtunnels.ms,primeiro.brs.devtunnels.ms,127.0.0.1,false"
            },
            nullValues = "null")
    void permiteSomenteOrigemDoDestinoAtualPorLoopback(
            String origin, String destination, String remote, boolean expected) throws Exception {
        var filter =
                new LoginProtecaoFilter("", "http://localhost:59999", mock(ProblemasApi.class));
        var request = new MockHttpServletRequest("POST", "/api/auth/renovar");
        request.setRemoteAddr(remote);
        if (origin != null) request.addHeader("Origin", origin);
        request.addHeader("Host", "localhost:59999");
        request.addHeader("X-Forwarded-Host", destination);
        request.addHeader("X-Forwarded-Proto", "https");
        var passed = new AtomicBoolean();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> passed.set(true));
        assertThat(passed.get()).isEqualTo(expected);
    }
}
