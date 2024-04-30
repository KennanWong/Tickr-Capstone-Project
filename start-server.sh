#!/usr/bin/sh

WEBHOOK_URL="http://localhost:8080/api/payment/webhook"
STRIPE_API_KEY="sk_test_51Ltt1uArvJ5MXKVUcYk5wKKUQwqFAsCq0zkmlnI96rB2CRdqtAWqS4EdckBPLLXMaJ7eoYyDEybFrkAlbPc6CXLw00chnKSWQn"


STRIPE_SECRET=$(stripe listen --api-key $STRIPE_API_KEY --print-secret)

stripe listen --api-key $STRIPE_API_KEY --forward-to WEBHOOK_URL &
proc_pid=$!
echo $proc_pid

cd backend
./backend.sh -live-email -live-stripe-secret=$STRIPE_SECRET

pkill proc_pid
