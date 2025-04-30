package oracleai;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/gdb")
public class GloballyDistributeDatabaseController {

//@Autowired
//javax.sql.DataSource dataSource;

    @GetMapping("/test")
    @ResponseBody
    public String textsearch() {
        return "{\"message\": \"This is a JSON response\"}";
    }
//    public String textsearch() throws Exception{
////    public String textsearch(Model model) throws Exception{
//        return "resultspage";
//    }

}