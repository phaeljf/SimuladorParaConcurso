package br.com.raphael.simuladorparaconcurso.web;

import br.com.raphael.simuladorparaconcurso.modelo.ProfSessao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;

@Component // <<< importante
public class ProfAuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws IOException {
        String uri = req.getRequestURI();
        if (uri.equals("/prof/login")) return true;
        ProfSessao sess = (ProfSessao) req.getSession().getAttribute("PROF_AUTH");
        if (sess != null) return true;
        resp.sendRedirect(req.getContextPath() + "/prof/login");
        return false;
    }
}
