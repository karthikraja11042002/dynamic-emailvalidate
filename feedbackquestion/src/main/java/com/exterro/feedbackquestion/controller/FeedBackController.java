package com.exterro.feedbackquestion.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.exterro.feedbackquestion.entity.FeedBackEntity;
import com.exterro.feedbackquestion.entity.QuestionEntity;
import com.exterro.feedbackquestion.entity.UserEntity;
import com.exterro.feedbackquestion.request.EmailRequest;
import com.exterro.feedbackquestion.services.FeedBackServices;
import com.exterro.feedbackquestion.services.QuestionServices;
import com.exterro.feedbackquestion.services.UserServices;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class FeedBackController {
    private final JavaMailSender javaMailSender;
    private static final Logger logger = LoggerFactory.getLogger(FeedBackController.class);

    @Autowired
    private FeedBackServices feedBackServices;
    
    @Autowired
    private QuestionServices questionServices;
 

    @Autowired
    private UserServices userServices;

    private static final String SECRET_KEY = "YourSecretKey123"; 

    @Autowired
    public FeedBackController(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @GetMapping("/home")
    @ResponseBody
    public String getData() {
        String str = "<a href='admin.html'>ADMIN</a><br><br><a href='feedback.html'>USER</a>";
        return str;
    }

    // Other methods...

    // Encryption method
    private String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            logger.error("Error while encrypting: " + e.toString());
        }
        return null;
    }

    // Decryption method
    private String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            logger.error("Error while decrypting: " + e.toString());
        }
        return null;
    }

    @PostMapping(path = "/submitRegistration")
    @ResponseBody
    public ResponseEntity<String> submitRegistration(@RequestParam String userData, HttpServletResponse response)
            throws JsonMappingException, JsonProcessingException {
        logger.info("Accessing submitRegistration method");
        ObjectMapper objmap = new ObjectMapper();
        UserEntity user = objmap.readValue(userData, UserEntity.class);
        userServices.addUser(user) ;

        Cookie idCookie = new Cookie("userId", encrypt(Integer.toString(user.getUserId())));
	     idCookie.setMaxAge(3600);
	     response.addCookie(idCookie);
	     System.out.println("@@@@@@@@@@@@@$###" + user.getUserId());

        return ResponseEntity.ok("questions.html");
    }

    @PostMapping(path = "/submitFeedback")
    @ResponseBody
    public ResponseEntity<String> submitFeedback(@RequestParam String userAnswers, HttpServletRequest request,
            HttpServletResponse response) throws JsonMappingException, JsonProcessingException {
        logger.info("Accessing submitFeedback method");
        ObjectMapper objmap = new ObjectMapper();
        String[] answers = objmap.readValue(userAnswers, String[].class);
        FeedBackEntity feedback = new FeedBackEntity();
        feedback.setAnswers(answers);
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%" + Arrays.toString(answers));
        
        // Get the userId from cookies
        Cookie[] cookies = request.getCookies();
        String userId = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("userId")) {
                    userId = decrypt(cookie.getValue());
                    break;
                }
            }
        }
        int verifyID = Integer.parseInt(userId);
        
        // Set the userId in the feedback entity
        if (verifyID != 0) {
            UserEntity user = new UserEntity();
            user.setUserId(verifyID);
            feedback.setUserId(user);
            feedBackServices.addFeedBack(feedback);
            
            Cookie userCode = new Cookie("userCode", encrypt(Integer.toString(feedback.getFeedBackId())));
            userCode.setMaxAge(3600);
            response.addCookie(userCode);
        }
        
        return ResponseEntity.ok("results.html");
    }

    @GetMapping("admin/viewFeedback")
    @ResponseBody
    public String viewFeedBack() {
        logger.info("Accessing viewFeedback method");

        StringBuilder feedBack = new StringBuilder();
        feedBack.append("<h1>View all answers</h1>");
        feedBack.append("<table border='1px'>");
        feedBack.append("<tr><th>UserName</th>");

        try {
            List<FeedBackEntity> feedbackList = feedBackServices.viewAllFeedBackDetails();
            
            // Dynamically add answer headers based on the length of the answers array
            if (!feedbackList.isEmpty()) {
                int numAnswers = feedbackList.get(0).getAnswers().length;
                for (int i = 1; i <= numAnswers; i++) {
                    feedBack.append("<th>Answer ").append(i).append("</th>");
                }
            }
            
            feedBack.append("</tr>");

            for (FeedBackEntity feedback : feedbackList) {
                feedBack.append("<tr>");
                feedBack.append("<td>").append(feedback.getUserId().getUserName()).append("</td>");
                String[] answers = feedback.getAnswers();
                
                for (String answer : answers) {
                    feedBack.append("<td>").append(answer).append("</td>");
                }
                
                feedBack.append("</tr>");
            }
        } catch (Exception e) {
            logger.error("An error occurred while retrieving feedback details", e);
        }

        feedBack.append("</table>");
        return feedBack.toString();
    }

//    @GetMapping("admin/viewFeedbackbyEmail")
//	@ResponseBody
//	public String viewFeedbackByEmail(@RequestParam String userEmail) {
//		logger.info("Accessing viewFeedbackByEmail method");
//
//		StringBuilder feedBack = new StringBuilder();
//		feedBack.append("<h1>View answers by email</h1>");
//		feedBack.append("<table border='1px'>");
//		feedBack.append(
//				"<tr><th>UserName</th><th>Answer 1</th><th>Answer 2</th><th>Answer 3</th><th>Answer 4</th><th>Answer 5</th></tr>");
//
//		try {
//			FeedBackEntity user = feedBackServices.viewFeedBack(userEmail);
//
//			feedBack.append("<tr>");
//			feedBack.append("<td>").append(user.getUserId().getUserEmail()).append("</td>");
//			feedBack.append("<td>").append(user.getAnswer1()).append("</td>");
//			feedBack.append("<td>").append(user.getAnswer2()).append("</td>");
//			feedBack.append("<td>").append(user.getAnswer3()).append("</td>");
//			feedBack.append("<td>").append(user.getAnswer4()).append("</td>");
//			feedBack.append("<td>").append(user.getAnswer5()).append("</td>");
//			feedBack.append("</tr>");
//		} catch (Exception e) {
//			logger.error("An error occurred while retrieving feedback details by email", e);
//		}
//
//		feedBack.append("</table>");
//		return feedBack.toString();
//	}
    @GetMapping("/getResult")
    public ResponseEntity<FeedBackEntity> getReport(@CookieValue(value = "userCode") String sid) {
        String decryptedSid = decrypt(sid);
        int feedbackId = Integer.parseInt(decryptedSid);
        FeedBackEntity feedback = feedBackServices.viewFeedBackById(feedbackId);
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!"+feedback.getUserId().getUserEmail());
        return ResponseEntity.ok(feedback);
    }
    @GetMapping("/getCookies")
    public ResponseEntity<UserEntity> getCookies(@CookieValue(value = "userId") String sid) {
    	String decryptedSid = decrypt(sid);
        int userId = Integer.parseInt(decryptedSid);
        System.out.println("@@@@@@@#######@@"+userId);
        UserEntity user = userServices.viewUserbyId(userId);
        return ResponseEntity.ok(user);
    }


    

    @PostMapping("/getEmail")	
    @ResponseBody
    public ResponseEntity<String> sendEmail(@RequestParam("emailAnswers") String emailAnswers) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String[] answers = objectMapper.readValue(emailAnswers, String[].class);
            String recipientEmail = answers[0];
            System.out.println("$$$$$$$$$$$$" + recipientEmail);
            String subject = "FeedBack Results";
            StringBuilder body = new StringBuilder();
            body.append("Hi,").append("\n\n");
            body.append("Your FeedBack Results Are:\n\n");
            List<QuestionEntity> questions = questionServices.viewAllQuestion();

            // Generate table
            body.append("<table style='border-collapse: collapse; border: 1px solid black;'>");
            body.append("<tr><th style='border: 1px solid black;'>Question</th><th style='border: 1px solid black;'>Answer</th></tr>");

            for (int i = 0; i < questions.size(); i++) {
                QuestionEntity question = questions.get(i);
                String questionText = question.getQuestionValue();
                String answer = answers[i + 1];

                body.append("<tr><td style='border: 1px solid black;'>").append(questionText).append("</td><td style='border: 1px solid black;'>").append(answer).append("</td></tr>");
            }

            body.append("</table>");

            if (recipientEmail == null || subject == null || body.toString() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing email details.");
            }

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body.toString(), true);
            javaMailSender.send(message);
            return ResponseEntity.ok("emailsuccessfull.html");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email data format.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email.");
        }
    }

}


