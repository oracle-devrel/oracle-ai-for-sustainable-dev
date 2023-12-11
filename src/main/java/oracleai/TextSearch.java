package oracleai;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/textsearch")
public class TextSearch {

    @GetMapping("/textsearch")
    public String picturestory1(Model model) throws Exception {
        String explanationOfResults = "no search results";
        model.addAttribute("results", explanationOfResults);
        return "resultspage";
    }

}
