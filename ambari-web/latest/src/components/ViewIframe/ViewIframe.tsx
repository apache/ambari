/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';

interface ViewIframeProps {
    baseUrl?: string;
}

const ViewIframe: React.FC<ViewIframeProps> = ({ baseUrl }) => {
    const { viewName, viewVersion, instanceName, viewPath = '' } = useParams<{
        viewName: string;
        viewVersion: string;
        instanceName: string;
        viewPath: string;
    }>();

    const iframeRef = useRef<HTMLIFrameElement>(null);
    const [iframeHeight, setIframeHeight] = useState<string>('100vh');
    const [isLoading, setIsLoading] = useState(true);
    const [hasError, setHasError] = useState(false);

    const getIframeSrc = () => {
        const protocol = window.location.protocol;
        const host = window.location.host;
        const urlBase = baseUrl || `${protocol}//${host}`;
        
        // Remove /main from the URL and ensure proper path construction
        let viewUrl = `${urlBase}/views/${viewName}/${viewVersion}/${instanceName}`;
        //TODO: know the logic behind having viewPath
        // Add any additional path components
        if (viewPath) {
            viewUrl += viewPath.startsWith('/') ? viewPath : `/${viewPath}`;
        }else{
            viewUrl += '/';
        }

        return viewUrl;
    };

    const handleIframeLoad = () => {
        setIsLoading(false);
        try {
            // Try to access iframe content - if this fails, it means the page didn't load properly
            if (iframeRef.current?.contentWindow?.location.href) {
                setHasError(false);
                resizeIframe();
            } else {
                setHasError(true);
            }
        } catch (error) {
            // Cross-origin error or other loading issues
            setHasError(true);
            console.error('Error loading iframe content:', error);
        }
    };

    const resizeIframe = () => {
        if (!iframeRef.current) return;

        try {
            const iframe = iframeRef.current;
            iframe.style.height = 'auto';

            if (
                iframe.contentWindow &&
                iframe.contentWindow.document &&
                iframe.contentWindow.document.body
            ) {
                const iframeContentBody = iframe.contentWindow.document.body;
                const contentHeight = iframeContentBody.scrollHeight;
                const windowHeight = window.innerHeight;
                const newHeight = Math.max(contentHeight, windowHeight - 100) + 'px';
                setIframeHeight(newHeight);
            }
        } catch (error) {
            // Ignore cross-origin errors during resize
            console.debug('Could not resize iframe (possibly cross-origin):', error);
        }
    };

    useEffect(() => {
        const interval = setInterval(resizeIframe, 2000);
        window.addEventListener('resize', resizeIframe);

        return () => {
            clearInterval(interval);
            window.removeEventListener('resize', resizeIframe);
        };
    }, []);

    if (hasError) {
        return null; // Render nothing if the URL is not working
    }

    return (
        <div className="d-flex flex-column w-100 h-100 overflow-hidden">
            {isLoading && (
                <div className="d-flex justify-content-center align-items-center p-4">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                </div>
            )}
            <iframe
                ref={iframeRef}
                src={getIframeSrc()}
                className={`w-100 border-0 flex-grow-1 ${isLoading ? 'd-none' : ''}`}
                style={{ height: iframeHeight, minHeight: '500px' }}
                seamless
                allowFullScreen
                onLoad={handleIframeLoad}
                title={`${viewName} View`}
            />
        </div>
    );
};

export default ViewIframe;