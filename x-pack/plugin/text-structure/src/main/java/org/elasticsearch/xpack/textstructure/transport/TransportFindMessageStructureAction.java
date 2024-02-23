/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.textstructure.transport;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.textstructure.action.FindMessageStructureAction;
import org.elasticsearch.xpack.core.textstructure.action.FindStructureResponse;
import org.elasticsearch.xpack.textstructure.structurefinder.TextStructureFinder;
import org.elasticsearch.xpack.textstructure.structurefinder.TextStructureFinderManager;
import org.elasticsearch.xpack.textstructure.structurefinder.TextStructureOverrides;
import org.elasticsearch.xpack.textstructure.structurefinder.TimeoutChecker;

import java.util.ArrayList;

import static org.elasticsearch.threadpool.ThreadPool.Names.GENERIC;

public class TransportFindMessageStructureAction extends HandledTransportAction<FindMessageStructureAction.Request, FindStructureResponse> {

    private final ThreadPool threadPool;

    @Inject
    public TransportFindMessageStructureAction(TransportService transportService, ActionFilters actionFilters, ThreadPool threadPool) {
        super(
            FindMessageStructureAction.NAME,
            transportService,
            actionFilters,
            FindMessageStructureAction.Request::new,
            EsExecutors.DIRECT_EXECUTOR_SERVICE
        );
        this.threadPool = threadPool;
    }

    @Override
    protected void doExecute(Task task, FindMessageStructureAction.Request request, ActionListener<FindStructureResponse> listener) {
        threadPool.executor(GENERIC).execute(() -> {
            try {
                listener.onResponse(buildTextStructureResponse(request));
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private FindStructureResponse buildTextStructureResponse(FindMessageStructureAction.Request request) throws Exception {
        TextStructureFinderManager structureFinderManager = new TextStructureFinderManager(threadPool.scheduler());
        try (TimeoutChecker timeoutChecker = new TimeoutChecker("structure analysis", request.getTimeout(), threadPool.scheduler())) {
            TextStructureFinder textStructureFinder = structureFinderManager.makeBestStructureFinder(
                new ArrayList<>(),
                request.getMessages(),
                new TextStructureOverrides(request),
                timeoutChecker
            );

            return new FindStructureResponse(textStructureFinder.getStructure());
        }
    }
}
