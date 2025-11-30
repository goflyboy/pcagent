package com.pcagent.util;

import com.pcagent.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 会话工具类
 */
@Slf4j
public class SessionUtils {
    /**
     * 生成下一个会话ID
     */
    public static String nextSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 创建会话
     */
    public static Session create(String sessionId, Plan plan) {
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setCurrentStep("");
        session.setNextAction(new NextAction());
        session.getNextAction().setType("execute");
        
        Progress progress = new Progress();
        progress.setCurrent(0);
        progress.setTotal(plan.getTasks().size());
        progress.setMessage("初始化会话");
        session.setProgress(progress);
        
        return session;
    }

    /**
     * 更新会话到下一步
     */
    public static Session updateSession4NextStep(Session currentSession, Object data, String step) {
        if (currentSession == null) {
            return null;
        }
        
        currentSession.setCurrentStep(step);
        currentSession.setData(data);
        
        if (currentSession.getProgress() != null) {
            int currentStepIndex = getStepIndex(step, currentSession.getProgress().getTotal());
            currentSession.getProgress().setCurrent(currentStepIndex);
        }
        
        return currentSession;
    }

    /**
     * 更新会话当前步骤（仅更新data）
     */
    public static Session updateSession4CurrentStep(Session currentSession, Object data) {
        if (currentSession != null) {
            currentSession.setData(data);
        }
        return currentSession;
    }

    /**
     * 获取步骤索引
     */
    private static int getStepIndex(String step, int total) {
        if (Plan.STEP1.equals(step)) {
            return 1;
        } else if (Plan.STEP2.equals(step)) {
            return 2;
        } else if (Plan.STEP3.equals(step)) {
            return 3;
        }
        return 0;
    }
}

