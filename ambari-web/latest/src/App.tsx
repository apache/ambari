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

import { Suspense } from "react";
import { createHashRouter, RouterProvider } from "react-router-dom";
import { HelmetProvider } from "react-helmet-async";
import { Toaster } from "react-hot-toast";
import RoutesList from "./router/RoutesList";
import { UserProvider } from "./store/UserContext";
import Spinner from "./components/Spinner";
import "./styles/app.scss";
import "./styles/console.scss";
import "./styles/theme.scss";
import { ThemeProvider } from "./store/ThemeContext";

const router = createHashRouter(RoutesList);

function App() {
  return (
    <ThemeProvider><HelmetProvider>
      <Suspense fallback={<Spinner />}>
        <UserProvider>
          <Toaster toastOptions={{ style: { background: "var(--console-surface)", color: "var(--console-ink)", border: "1px solid var(--console-line)" } }} />
          <RouterProvider router={router} />
        </UserProvider>
      </Suspense>
    </HelmetProvider></ThemeProvider>
  );
}

export default App;
