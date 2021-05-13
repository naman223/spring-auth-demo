package com.security.auth.springauthdemo.controller;

import com.security.auth.springauthdemo.entity.User;
import com.security.auth.springauthdemo.repository.UserRepository;
import com.security.auth.springauthdemo.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @PostMapping("/join")
    public String onBoardUser(@RequestBody User user) {
        user.setRoles(Constants.DEFAULT_ROLE);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return "Welcome to group "+user.getUserName()+" !!!";
    }

    @GetMapping("/access/{userId}/{userRole}")
        @PreAuthorize("hasAnyAuthority('ROLE_ADMIN') or hasAnyAuthority('ROLE_MODERATOR')")
    public String giveAccessToUser(@PathVariable int userId, @PathVariable String userRole, Principal principal) {
        User user = userRepository.findById(userId).get();
        List<String> rolesByLoggedInUser = getRolesByLoggedInUser(principal);

        String newRoles = "";
        if(rolesByLoggedInUser.contains(userRole)) {
            newRoles = user.getRoles()+","+userRole;
            user.setRoles(newRoles);
        }
        userRepository.save(user);
        return "Role is changed for user "+userId +" to new roles "+newRoles+" !!!";
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/test")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public String getMyDetails() {
        return "Welcome user accessed by All !!!";
    }

    private List<String> getRolesByLoggedInUser(Principal principal) {
        User user = getLoggedInUser(principal);
        List<String> userRoles = Arrays.stream(user.getRoles().split(",")).collect(Collectors.toList());
        if(userRoles.contains("ROLE_ADMIN")) {
            return Arrays.stream(Constants.ADMIN_ACCESS).collect(Collectors.toList());
        } else if(userRoles.contains("ROLE_MODERATOR")) {
            return Arrays.stream(Constants.MODERATOR_ACCESS).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private User getLoggedInUser(Principal principal) {
        return userRepository.findByUserName(principal.getName()).get();
    }
}
