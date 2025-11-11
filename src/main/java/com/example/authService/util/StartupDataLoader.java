// package com.example.authService.util;


// import java.util.Set;

// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;

// import com.example.authService.entity.Role;
// import com.example.authService.service.UserService;

// @Component
// public class StartupDataLoader implements CommandLineRunner {

//     private final UserService userService;

//     public StartupDataLoader(UserService userService) {
//         this.userService = userService;
//     }

//     @Override
//     public void run(String... args) throws Exception {
//         // Create default users if not present
//         try {
//             if (userService.findByUsername("admin").isEmpty()) {
//                 userService.createUser("admin", "Admin@123", Set.of(Role.ROLE_ADMIN));
//                 System.out.println("Created default admin/admin (password Admin@123)");
//             }
//             if (userService.findByUsername("manager").isEmpty()) {
//                 userService.createUser("manager", "Manager@123", Set.of(Role.ROLE_MANAGER));
//                 System.out.println("Created default manager/Manager@123");
//             }
//             if (userService.findByUsername("user").isEmpty()) {
//                 userService.createUser("user", "User@123", Set.of(Role.ROLE_USER));
//                 System.out.println("Created default user/User@123");
//             }
//         } catch (Exception ex) {
//             System.err.println("Startup user creation failed: " + ex.getMessage());
//         }
//     }
// }

