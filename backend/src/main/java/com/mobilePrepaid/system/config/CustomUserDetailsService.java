package com.mobilePrepaid.system.config;

import com.mobilePrepaid.system.model.Admin;
import com.mobilePrepaid.system.model.Subscriber;
import com.mobilePrepaid.system.repository.AdminRepository;
import com.mobilePrepaid.system.repository.SubscriberRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final SubscriberRepository subscriberRepository;

    public CustomUserDetailsService(AdminRepository adminRepository, SubscriberRepository subscriberRepository) {
        this.adminRepository = adminRepository;
        this.subscriberRepository = subscriberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // First try admin
        Admin admin = adminRepository.findByUsername(username);
        if (admin != null) {
            return new User(
                admin.getUsername(),
                admin.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(admin.getRole().name()))
            );
        }

        // Then try subscriber
        Subscriber subscriber = subscriberRepository.findByMobileNumber(username);
        if (subscriber != null) {
            return new User(
                subscriber.getMobileNumber(),
                subscriber.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("SUBSCRIBER"))
            );
        }

        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}