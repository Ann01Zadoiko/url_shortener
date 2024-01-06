package cc.worldmandia.web;

import cc.worldmandia.security.auth.AuthenticationServiceImpl;
import cc.worldmandia.security.auth.request.LogInRequest;
import cc.worldmandia.security.auth.request.SignUpRequest;
import cc.worldmandia.security.auth.response.JwtAuthenticationResponse;
import cc.worldmandia.url.Url;
import cc.worldmandia.user.UserRegisterDto;
import cc.worldmandia.util.UrlValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cc.worldmandia.web.WebConstants.*;


@RequiredArgsConstructor
@Controller
@RequestMapping("/url-shortener")
public class UrlController {
    private final UrlServiceImpl urlService;
    private final AuthenticationServiceImpl authenticationService;
    private String timeUrl;
    private final String redirectToList = "redirect:/url-shortener/list";
    @GetMapping
    public String start() {
        return "main";
    }

    @PostMapping("/reklama")
    public String getAd(@RequestParam ("shortUrl") String shortUrl) {
        timeUrl = shortUrl;
        return "ad";
    }

    @PostMapping("/goToUrl")
    public String go() {
        Url url = urlService.findURLWithUsersByShortURL(timeUrl);
        if(url.isEnabled()){
            urlService.incrementClickCount(url.getId());
            return "redirect:" + url.getFullUrl();
        }
            return "404";
    }

    @GetMapping("/list")
    public String getUrlList(Model model) {
        List<Url> urlList = urlService.findAll();
        model.addAttribute("urls", urlList);
        return "stats";
    }

    @GetMapping("/create")
    public String createUrlPage(Model model) {
        model.addAttribute("createUrl", new Url());
        return "newUrl";
    }

    @PostMapping("/create")
    public String createShortUrl(@RequestParam String fullUrl,
                                 @RequestParam String title,
                                 @RequestParam String description,
                                 @ModelAttribute Url newUrl, Model model) {
        if(fullUrl == null || fullUrl.isEmpty()||fullUrl.length()> MAX_LENGTH|| !UrlValidator.validUrl(fullUrl)) {
            model.addAttribute("errorFullUrl", INVALID_URL);
            return "newUrl";
        }
        if(title == null || title.isEmpty() || title.length()>MAX_LENGTH){
            model.addAttribute("errorTitle", INVALID_TITLE);
            return "newUrl";
        }
        if(description.length()>MAX_LENGTH){
            model.addAttribute("errorDescription", INVALID_DESCRIPTION);
            return "newUrl";
        }
        // треба передавати сюди емейл юзера!!!!
        urlService.createUrl(newUrl, "example1@gmail.com");
        return redirectToList;
    }

    @GetMapping("/edit")
    public String editUrl(@RequestParam long id, Model model) {
        Url searchUrl = urlService.findById(id);
        model.addAttribute("url", searchUrl);
        return "editUrl";
    }

    @PostMapping("/edit")
    public String editUrl(@RequestParam String fullUrl,
                          @RequestParam String title,
                          @RequestParam String description,
                          @ModelAttribute Url editUrl, Model model) {
        if(fullUrl == null || fullUrl.isEmpty()||fullUrl.length()> MAX_LENGTH) {
            model.addAttribute("errorFullUrl", INVALID_URL);
            return "editUrl";
        }
        if(title == null || title.isEmpty() || title.length()>MAX_LENGTH){
            model.addAttribute("errorTitle", INVALID_TITLE);
            return "editUrl";
        }
        if(description.length()>MAX_LENGTH){
            model.addAttribute("errorDescription", INVALID_DESCRIPTION);
            return "editUrl";
        }
        urlService.updateTitleOrDescription(editUrl);
        return redirectToList;
    }

    @PostMapping("/delete")
    public String deleteUrl(@RequestParam long id) {
        urlService.deleteById(id);
        return redirectToList;
    }

    @PostMapping("/checked")
    public String updateEnabledStatus(@ModelAttribute Url url) {
        urlService.updateEnabledStatus(url);
        return redirectToList;
    }

    @PostMapping("/prolong")
    public String prolongEndDate(@ModelAttribute Url url) {
        urlService.prolongEndDate(url);
        return redirectToList;
    }

    // registration controller
    @GetMapping("/registration")
    public String redirectToRegistrationForm(Model model){
        UserRegisterDto userRegisterDto = new UserRegisterDto();
        model.addAttribute(userRegisterDto);
        return "/registration";
    }
    @PostMapping("/registration")
    public String registeringUser(@ModelAttribute("user")@Valid UserRegisterDto userRegisterDto, Model model){

        JwtAuthenticationResponse response= authenticationService.signup(new SignUpRequest(
                userRegisterDto.getEmail(),
                userRegisterDto.getUsername(),
                userRegisterDto.getPassword(),
                userRegisterDto.getRepeatedPassword()
        ));
        if(!userRegisterDto.getPassword().equals(userRegisterDto.getRepeatedPassword())){
            model.addAttribute("userRegisterDto", userRegisterDto);
            model.addAttribute("statusCode", response.getMessage());
            return "/registration";
        }
        if(response.getStatus() == 200) {
            model.addAttribute("user", userRegisterDto);
            return "registerSuccess";
        }

        model.addAttribute("statusCode", response.getStatus());
        model.addAttribute("userRegisterDto", userRegisterDto);
        return "/registration";
    }
    @GetMapping("/login")
    public String redirectToLoginForm(Model model){
        UserRegisterDto userRegisterDto = new UserRegisterDto();
        model.addAttribute(userRegisterDto);
        return "/login";
    }
    @PostMapping("/login")
    public String login(@ModelAttribute("user")@Valid UserRegisterDto userRegisterDto, Model model){
        JwtAuthenticationResponse response= authenticationService.login(new LogInRequest(
                userRegisterDto.getEmail(),
                userRegisterDto.getPassword()
        ));
        if(response.getStatus() == 200) {
            model.addAttribute("token", response.getToken());
            return start();
        }
        model.addAttribute("statusCode", response.getMessage());
        model.addAttribute("userRegisterDto", userRegisterDto);
        return "login";

    }
}
