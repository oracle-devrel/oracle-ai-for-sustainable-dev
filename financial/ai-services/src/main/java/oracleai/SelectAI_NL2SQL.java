package oracleai;

import com.oracle.bmc.aivision.model.FaceDetectionFeature;
import oracleai.services.OracleVisionAI;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/selectai")
public class SelectAI_NL2SQL {

//@Autowired
//javax.sql.DataSource dataSource;

    @PostMapping("/test")
    public String textsearch(Model model) throws Exception{
//        System.out.println("SelectAI_NL2SQL.test dataSource:" + dataSource);
//        model.addAttribute("results", dataSource.getConnection());
        return "resultspage";
    }

}