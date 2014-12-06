package org.hotswap.agent.plugin.wicket.app.services;

import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {
	
	@Override
	public int getNumber() {
		return 1;
	}
}
