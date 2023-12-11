package oracleai;


import oracleai.services.ORDSCalls;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/textsearch")
public class TextSearch {

    @PostMapping("/textsearch")
    public String textsearch(@RequestParam("sql") String sql, Model model) {
        String explanationOfResults = ORDSCalls.executeDynamicSQL(
                AIApplication.ORDS_ENDPOINT_EXECUTE_DYNAMIC_SQL, sql);
        model.addAttribute("results", explanationOfResults);
        return "resultspage";
    }

}
