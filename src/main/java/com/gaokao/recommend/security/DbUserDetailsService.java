package com.gaokao.recommend.security;

import com.gaokao.recommend.entity.UserAccount;
import com.gaokao.recommend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public DbUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("账号不存在"));
        return new User(account.getUsername(), account.getPassword(), account.getEnabled(), true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority(account.getRole())));
    }
}
