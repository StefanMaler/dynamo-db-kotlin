
# Table of contents
1. [documentation](#documentation)
2. [AWS CLI examples](#aws-cli-examples)
3. [TODOs](#todos)

## documentation

[docker-compose](docker-compose.yml)

[documentation on localstack configuration via environment variables](https://docs.localstack.cloud/localstack/configuration/)

> EDGE_PORT 4566 (default)

[aws-cli to access local stack](https://docs.localstack.cloud/integrations/aws-cli/)

## AWS CLI examples

```bash
docker compose up localstack
...
... [INFO] Running on https://0.0.0.0:4566 (CTRL + C to quit)
...

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
alias awslocal='aws --endpoint-url=http://localhost:4566'
  
awslocal dynamodb list-tables
    {
        "TableNames": []
    }
awslocal cloudformation deploy \
  --stack-name dynamo-db-experiment-stack \
  --template-file cloudformation/customer_table.yaml
awslocal cloudformation list-stacks
{
    "StackSummaries": [
        {
            "StackId": "arn:aws:cloudformation:eu-central-1:000000000000:stack/dynamo-db-experiment-stack/603ed07c",
            "StackName": "dynamo-db-experiment-stack",
            "CreationTime": "2022-05-16T11:08:48.303000+00:00",
            "StackStatus": "CREATE_COMPLETE",
            "StackStatusReason": "Deployment succeeded"
        }
    ]
}
$ awslocal dynamodb list-tables
{
    "TableNames": [
        "Customer"
    ]
}
$ awslocal dynamodb scan --table-name Customer
{
    "Items": [],
    "Count": 0,
    "ScannedCount": 0,
    "ConsumedCapacity": null
}
$ awslocal dynamodb describe-table --table-name Customer
...
    "AttributeDefinitions": [
        {
            "AttributeName": "id",
            "AttributeType": "S"
        }
    ],
    "TableName": "Customer",
    "KeySchema": [
        {
            "AttributeName": "id",
            "KeyType": "HASH"
        }
    ],
...
# https://docs.aws.amazon.com/cli/latest/reference/dynamodb/put-item.html
# note:
#   "id" is required - primary key(hash) in the tables schema
#   upsert operation; overwrites/replaces an item with the same primary key
$ awslocal dynamodb put-item --table-name 'Customer' --item '
{
  "id": {
    "S": "1"
  },
  "arbitraryNested": {
    "M": {
        "propertyA" : {
          "S": "valueA"
        },
        "propertyB" : {
          "N": "123"
        }
    }
  },
  "arbitraryList": {
    "L": [
      { "S": "first" },
      { "S": "second" } 
    ]
  }
}
'
$ awslocal dynamodb get-item --table-name Customer --key '{
  "id": {
    "S": "1"
  }
}'
# https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html
# can't inline values into update-expression, it seems. Setting --update-expression 'SET att = {"S":"some-value"}' fails.
# See: https://stackoverflow.com/questions/32835299/awscli-dynamodb-update-item-command-syntax
$ awslocal dynamodb update-item --table-name 'Customer' --key '
{
  "id": {
    "S": "1"
  }
}
' \
--update-expression 'SET att = :h' \
--expression-attribute-values '{":h":{"S":"some-value"}}'
$ awslocal dynamodb update-item --table-name 'Customer' --key '
{
  "id": {
    "S": "1"
  }
}
' \
--update-expression 'REMOVE att_mat'
# note: field name cannot contain '-' unless specified via --expression-attribute-names option; --update-expression 'SET "some-property" = :h' fails
# '_' does not have this issue
$ awslocal dynamodb update-item --table-name 'Customer' --key '
{
  "id": {
    "S": "1"
  }
}
' \
--update-expression 'SET #H = :h' \
--expression-attribute-names '{"#H":"some-property"}' \
--expression-attribute-values '{":h":{"S":"some-value"}}'
$ awslocal dynamodb delete-item --table-name Customer --key '{
  "id": {
    "S": "1"
  }
}'
```

## TODOs

### write json documents directly to dynamo db

* not possible via [aws cli put-item](https://docs.aws.amazon.com/cli/latest/reference/dynamodb/put-item.html) - see examples in (README)[#AWS CLI examples]
* not possible with [AWS SDK 2 for java](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb.html)
* possible with AWS SDK [2](https://docs.aws.amazon.com/sdk-for-javascript/v2/developer-guide/welcome.html) and [3](https://docs.aws.amazon.com/AWSJavaScriptSDK/v3/latest/index.html) for [java script](https://docs.aws.amazon.com/AWSJavaScriptSDK/latest/AWS/DynamoDB/DocumentClient.html#put-property) and [.NET](https://docs.aws.amazon.com/sdk-for-net/v3/developer-guide/dynamodb-json.html)

ideas for a workaround:
* parse json and map to a @DynamoDbBean annotated class
* issue: [Preserve JSON helper methods in the DynamoDB Document API](https://github.com/aws/aws-sdk-java-v2/issues/1862) mentions json support was present in java SDK 1 but no longer in AWS SDK 2 and links a [pull request](https://github.com/aws/aws-sdk-java-v2/pull/3060) that offers some step towards json support (opened Feb 28, 2022)

### configure logging

currently logs are very verbose

### check isolation of tests

separate tests should be isolated with regards to localstack testcontainers

### explore dynamo db best practices

https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html

### explore NoSQL workbench

https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/workbench.html
