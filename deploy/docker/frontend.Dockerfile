# Build all four Vue applications once, then serve their static files from Nginx.
FROM node:22.23.1-alpine3.24 AS build
WORKDIR /workspace

COPY package.json ./
COPY frontend-shared/package.json frontend-shared/package.json
COPY web_portal/package.json web_portal/package.json
COPY vue_student/package.json vue_student/package.json
COPY vue_admin/package.json vue_admin/package.json
COPY vue_superadmin/package.json vue_superadmin/package.json
# The legacy peer flag is required by a few Vue CLI-era packages until they are upgraded.
RUN npm install --legacy-peer-deps --no-audit --no-fund

COPY frontend-shared frontend-shared
COPY web_portal web_portal
COPY vue_student vue_student
COPY vue_admin vue_admin
COPY vue_superadmin vue_superadmin

# The portal uses these URLs when users choose an identity.
ARG VUE_APP_STUDENT_URL=http://localhost:9533/#/login
ARG VUE_APP_ADMIN_URL=http://localhost:9531/#/login
ARG VUE_APP_SUPERADMIN_URL=http://localhost:9532/#/login
ENV VUE_APP_STUDENT_URL=$VUE_APP_STUDENT_URL \
    VUE_APP_ADMIN_URL=$VUE_APP_ADMIN_URL \
    VUE_APP_SUPERADMIN_URL=$VUE_APP_SUPERADMIN_URL

RUN npm run build:frontends

FROM nginx:1.30.4-alpine

COPY deploy/nginx/docker.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/web_portal/dist /usr/share/nginx/html/portal
COPY --from=build /workspace/vue_student/dist /usr/share/nginx/html/student
COPY --from=build /workspace/vue_admin/dist /usr/share/nginx/html/admin
COPY --from=build /workspace/vue_superadmin/dist /usr/share/nginx/html/superadmin

EXPOSE 9527 9533 9531 9532
